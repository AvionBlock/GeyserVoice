package team.avion.paper.voicecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import team.avion.paper.GeyserVoice;
import team.avion.protocol.VoiceCraftProtocol;
import team.avion.protocol.VoiceCraftRuntimeDefaults;

public class VoiceCraftProcessManager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private final GeyserVoice plugin;

    private Process process;

    public VoiceCraftProcessManager(GeyserVoice plugin) {
        this.plugin = plugin;
    }

    public synchronized void ensureRunning() {
        if (!plugin.getConfig().getBoolean("config.voicecraft.auto-start", true)) {
            return;
        }
        if (plugin.usesProxy) {
            return;
        }
        if (isEndpointReachable()) {
            return;
        }
        if (process != null && process.isAlive()) {
            return;
        }

        LaunchTarget launchTarget = null;
        try {
            launchTarget = resolveReleaseLaunchTarget();
            if (launchTarget == null) {
                process = null;
                return;
            }

            List<String> command = new ArrayList<>();
            command.add(launchTarget.command());
            command.addAll(launchTarget.arguments());
            appendRuntimeArguments(command);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(launchTarget.workingDirectory().toFile());
            configureRuntimeEnvironment(builder);

            plugin.Logger.info("Starting managed VoiceCraft process: " + launchTarget.command());
            process = builder.start();
            startLogPump(process.getInputStream(), false);
            startLogPump(process.getErrorStream(), true);
            waitUntilReachable();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            plugin.Logger.error("VoiceCraft startup was interrupted: " + ex.getMessage());
            process = null;
        } catch (IOException ex) {
            plugin.Logger.error("Failed to start VoiceCraft process: " + ex.getMessage());
            plugin.Logger.error("VoiceCraft executable path: " + (launchTarget == null ? "<unresolved>" : launchTarget.command()));
            process = null;
        }
    }

    public synchronized void shutdown() {
        if (process == null) {
            return;
        }

        if (plugin.getConfig().getBoolean("config.voicecraft.shutdown-on-disable", true)) {
            plugin.Logger.info("Stopping managed VoiceCraft process...");
            process.destroy();
            try {
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }

        process = null;
    }

    private Thread startLogPump(InputStream stream, boolean errorStream) {
        Thread thread = Thread.ofVirtual().name(errorStream ? "voicecraft-stderr" : "voicecraft-stdout").start(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (errorStream) {
                        plugin.Logger.warn("[VoiceCraft] " + line);
                    } else {
                        plugin.Logger.info("[VoiceCraft] " + line);
                    }
                }
            } catch (IOException ignored) {
            }
        });
        return thread;
    }

    private void appendRuntimeArguments(List<String> command) {
        command.add("--transport-mode");
        command.add("tcp");
        command.add("--transport-host");
        command.add(plugin.getHost());
        command.add("--transport-port");
        command.add(Integer.toString(plugin.getPort()));
        command.add("--server-key");
        command.add(plugin.getLoginToken());
    }

    private void configureRuntimeEnvironment(ProcessBuilder builder) {
        if (!plugin.getConfig().getBoolean("config.voicecraft.invariant-globalization", true)) {
            return;
        }

        builder.environment().put("DOTNET_SYSTEM_GLOBALIZATION_INVARIANT", "1");
    }

    private void waitUntilReachable() {
        long timeoutMs = plugin.getConfig().getLong("config.voicecraft.ready-timeout-ms", 20000L);
        Instant deadline = Instant.now().plus(Duration.ofMillis(timeoutMs));
        while (Instant.now().isBefore(deadline)) {
            if (process == null || !process.isAlive()) {
                plugin.Logger.error("Managed VoiceCraft process exited before it became reachable.");
                process = null;
                return;
            }

            if (isEndpointReachable()) {
                plugin.Logger.info("Managed VoiceCraft process is reachable.");
                return;
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        plugin.Logger.warn("VoiceCraft process started, but the transport endpoint is not reachable yet.");
    }

    private boolean isEndpointReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(plugin.getHost(), plugin.getPort()), 500);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private LaunchTarget resolveReleaseLaunchTarget() throws IOException, InterruptedException {
        Path installDirectory = resolveInstallDirectory();
        Files.createDirectories(installDirectory);

        String platform = defaultPlatform();
        String architecture = defaultArchitecture();
        String assetName = resolveAssetName(platform, architecture);
        String releaseId = VoiceCraftRuntimeDefaults.RELEASE + "|" + VoiceCraftProtocol.VERSION_STRING + "|" + assetName;
        Path releaseMarker = installDirectory.resolve(".geyservoice-release");
        Path executablePath = resolveManagedExecutable(installDirectory, platform);

        if (!Files.exists(executablePath) || !releaseId.equals(readReleaseMarker(releaseMarker))) {
            plugin.Logger.info("Preparing VoiceCraft runtime " + VoiceCraftRuntimeDefaults.RELEASE
                    + " for McApi protocol " + VoiceCraftProtocol.VERSION_STRING
                    + " (" + platform + "/" + architecture + ").");
            downloadAndExtractRelease(installDirectory, assetName);
            writeReleaseMarker(releaseMarker, releaseId);
        }

        if (!Files.exists(executablePath)) {
            plugin.Logger.error("Managed VoiceCraft executable was not found after extraction: " + executablePath);
            return null;
        }

        executablePath.toFile().setExecutable(true);
        Path absoluteExecutablePath = executablePath.toAbsolutePath().normalize();
        return new LaunchTarget(absoluteExecutablePath.getParent(), absoluteExecutablePath.toString(), List.of());
    }

    private Path resolveInstallDirectory() {
        String installDirectory = plugin.getConfig().getString("config.voicecraft.install-directory",
                VoiceCraftRuntimeDefaults.INSTALL_DIRECTORY);
        if (installDirectory == null || installDirectory.isBlank()) {
            installDirectory = VoiceCraftRuntimeDefaults.INSTALL_DIRECTORY;
        }

        Path path = Path.of(installDirectory);
        if (path.isAbsolute()) {
            return path;
        }
        return plugin.getDataFolder().toPath().resolve(path).toAbsolutePath().normalize();
    }

    private String resolveAssetName(String platform, String architecture) {
        return VoiceCraftRuntimeDefaults.EXECUTABLE_BASE_NAME + "." + capitalize(platform) + "." + architecture + ".zip";
    }

    private Path resolveManagedExecutable(Path installDirectory, String platform) {
        String fileName = "windows".equalsIgnoreCase(platform)
                ? VoiceCraftRuntimeDefaults.EXECUTABLE_BASE_NAME + ".exe"
                : VoiceCraftRuntimeDefaults.EXECUTABLE_BASE_NAME;
        return installDirectory.resolve(fileName).normalize();
    }

    private void downloadAndExtractRelease(Path installDirectory, String assetName) throws IOException, InterruptedException {
        String downloadUrl = buildReleaseUrl(VoiceCraftRuntimeDefaults.GITHUB_REPOSITORY,
                VoiceCraftRuntimeDefaults.RELEASE, assetName);

        plugin.Logger.info("Downloading VoiceCraft release asset " + assetName + "...");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " while downloading " + downloadUrl);
        }

        deleteDirectoryContents(installDirectory);
        try (InputStream body = response.body(); ZipInputStream zipInputStream = new ZipInputStream(body)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = installDirectory.resolve(entry.getName()).normalize();
                if (!target.startsWith(installDirectory)) {
                    throw new IOException("Refusing to extract outside of install directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }

                Files.createDirectories(target.getParent());
                try (OutputStream outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    zipInputStream.transferTo(outputStream);
                }
            }
        }
    }

    private static String buildReleaseUrl(String repo, String release, String assetName) {
        String releasePath = "latest".equalsIgnoreCase(release) ? "latest/download/" : "download/" + release + "/";
        return "https://github.com/" + repo + "/releases/" + releasePath + assetName;
    }

    private static void deleteDirectoryContents(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .filter(path -> !path.equals(directory))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw ex;
        }
    }

    private static String readReleaseMarker(Path releaseMarker) throws IOException {
        if (!Files.exists(releaseMarker)) {
            return "";
        }
        return Files.readString(releaseMarker, StandardCharsets.UTF_8).trim();
    }

    private static void writeReleaseMarker(Path releaseMarker, String releaseId) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(releaseMarker, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.write(releaseId);
        }
    }

    private static String defaultPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "macos";
        }
        return "linux";
    }

    private static String defaultArchitecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        }
        if (arch.contains("arm")) {
            return "arm";
        }
        if (arch.contains("86") && !arch.contains("64")) {
            return "x86";
        }
        return "x64";
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private record LaunchTarget(Path workingDirectory, String command, List<String> arguments) {
    }
}
