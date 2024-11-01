package io.greitan.avion.bungeecord.utils;

import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;

import io.greitan.avion.bungeecord.GeyserVoice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Language {
    private static final Map<String, Configuration> languageConfigs = new HashMap<>();
    private static String defaultLanguage = "en";

    public static void init(GeyserVoice plugin) {
        File languageFolder = new File(plugin.getDataFolder(), "locale");

        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
            plugin.saveResource("locale/en.yml");
            plugin.saveResource("locale/ru.yml");
            plugin.saveResource("locale/nl.yml");
        }

        loadLanguages(languageFolder.getAbsolutePath());
    }

    private static void loadLanguages(String pluginFolder) {
        File languageFolder = new File(pluginFolder);

        if (languageFolder.exists() && languageFolder.isDirectory()) {

            for (File file : languageFolder.listFiles()) {

                if (file.getName().endsWith(".yml")) {
                    String language = file.getName().replace(".yml", "");
                    try {
                        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                        languageConfigs.put(language, config);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*
     * private static void copyResource(Plugin plugin, String resourceName, File
     * destination)
     * {
     * try (InputStream inputStream = plugin.getResourceAsStream(resourceName))
     * {
     * if (inputStream != null)
     * {
     * Files.copy(inputStream, destination.toPath(),
     * StandardCopyOption.REPLACE_EXISTING);
     * }
     * } catch (IOException e)
     * {
     * e.printStackTrace();
     * }
     * }
     */

    public static String getMessage(String language, String key) {
        if (languageConfigs.containsKey(language)) {
            Configuration config = languageConfigs.get(language);
            if (config.contains("messages." + key)) {
                return config.getString("messages." + key);
            }
        }
        return languageConfigs.get(defaultLanguage).getString("messages." + key);
    }
}
