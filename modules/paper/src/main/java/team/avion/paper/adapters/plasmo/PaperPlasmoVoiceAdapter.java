package team.avion.paper.adapters.plasmo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import team.avion.adapter.AdapterContext;
import team.avion.adapter.AdapterState;
import team.avion.adapter.VoiceAdapter;
import team.avion.adapter.plasmo.PlasmoChannels;
import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.session.PlasmoClientSession;
import team.avion.adapter.plasmo.session.PlasmoSessionManager;
import team.avion.adapter.plasmo.tcp.PlasmoTcpCodec;
import team.avion.adapter.plasmo.tcp.PlasmoTcpCodecs;
import team.avion.adapter.plasmo.tcp.packet.ConfigPacket;
import team.avion.adapter.plasmo.tcp.packet.ConnectionPacket;
import team.avion.adapter.plasmo.tcp.packet.PlayerInfoPacket;
import team.avion.adapter.plasmo.tcp.packet.PlayerInfoRequestPacket;
import team.avion.adapter.plasmo.tcp.packet.PlayerListPacket;
import team.avion.adapter.plasmo.udp.PlasmoUdpCodec;
import team.avion.adapter.plasmo.udp.PlasmoUdpCodecs;
import team.avion.adapter.plasmo.udp.PlasmoUdpEnvelope;
import team.avion.adapter.plasmo.udp.packet.PingPacket;
import team.avion.paper.GeyserVoice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PaperPlasmoVoiceAdapter implements VoiceAdapter, PluginMessageListener, Listener {
    private final GeyserVoice plugin;
    private final PlasmoTcpCodec tcpCodec = PlasmoTcpCodecs.createBaseCodec();
    private final PlasmoUdpCodec udpCodec = PlasmoUdpCodecs.createBaseCodec();
    private final UUID serverId = UUID.randomUUID();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private AdapterState state = AdapterState.NEW;
    private PlasmoSessionManager sessions;
    private DatagramSocket udpSocket;
    private Thread udpThread;
    private String bindHost;
    private String advertisedHost;
    private int udpPort;
    private int proximityDistance;

    public PaperPlasmoVoiceAdapter(GeyserVoice plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "plasmo";
    }

    @Override
    public String displayName() {
        return "Plasmo Voice";
    }

    @Override
    public AdapterState state() {
        return state;
    }

    @Override
    public void start(AdapterContext context) throws Exception {
        if (running.get()) {
            return;
        }

        state = AdapterState.STARTING;
        this.sessions = new PlasmoSessionManager(context.audioBridge());
        this.bindHost = plugin.getConfig().getString("config.adapters.plasmo.bind-host", "0.0.0.0");
        this.advertisedHost = plugin.getConfig().getString("config.adapters.plasmo.advertised-host", "0.0.0.0");
        this.udpPort = plugin.getConfig().getInt("config.adapters.plasmo.port", 24454);
        this.proximityDistance = plugin.getConfig().getInt("config.voice.proximity-distance", 30);

        udpSocket = new DatagramSocket(new InetSocketAddress(bindHost, udpPort));
        udpSocket.setSoTimeout(1000);
        running.set(true);
        udpThread = new Thread(this::runUdpLoop, "GeyserVoice-Plasmo-UDP");
        udpThread.setDaemon(true);
        udpThread.start();

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, PlasmoChannels.MAIN);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, PlasmoChannels.FLAG);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, PlasmoChannels.MAIN, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        state = AdapterState.RUNNING;
        plugin.Logger.info("Plasmo Voice adapter listening on " + bindHost + ":" + udpPort + ".");
    }

    @Override
    public void stop() {
        if (!running.getAndSet(false)) {
            state = AdapterState.STOPPED;
            return;
        }

        state = AdapterState.STOPPING;
        HandlerList.unregisterAll(this);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, PlasmoChannels.MAIN, this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, PlasmoChannels.MAIN);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, PlasmoChannels.FLAG);

        if (udpSocket != null) {
            udpSocket.close();
        }
        if (udpThread != null) {
            try {
                udpThread.join(2000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        state = AdapterState.STOPPED;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!PlasmoChannels.MAIN.equals(channel)) {
            return;
        }

        try {
            PlasmoPacket packet = tcpCodec.decode(message);
            if (packet instanceof PlayerInfoPacket infoPacket) {
                handlePlayerInfo(player, infoPacket);
            }
        } catch (Exception exception) {
            plugin.Logger.warn("Failed to decode Plasmo packet from " + player.getName() + ": " + exception.getMessage());
        }
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!PlasmoChannels.MAIN.equals(event.getChannel()) && !PlasmoChannels.FLAG.equals(event.getChannel())) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> requestPlayerInfo(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (sessions.byPlayerId(event.getPlayer().getUniqueId()).isPresent()) {
            sessions.remove(event.getPlayer().getUniqueId());
            plugin.getSessionManager().destroyEntityForPlayer(event.getPlayer().getName());
        }
    }

    private void requestPlayerInfo(Player player) {
        if (!running.get() || !player.isOnline()) {
            return;
        }
        sendTcpPacket(player, new PlayerInfoRequestPacket());
    }

    private void handlePlayerInfo(Player player, PlayerInfoPacket packet) {
        if (packet.voiceDisabled()) {
            return;
        }
        if (!plugin.isConnected()) {
            plugin.Logger.warn("Plasmo client " + player.getName() + " joined before VoiceCraft was connected.");
            return;
        }

        OptionalInt entityId = plugin.getSessionManager().createEntityForPlayer(player);
        if (entityId.isEmpty()) {
            plugin.Logger.warn("Failed to create VoiceCraft entity for Plasmo client " + player.getName() + ".");
            return;
        }

        PlasmoClientSession session = sessions.createOrReplace(player.getUniqueId(), player.getName(), entityId.getAsInt());
        sendTcpPacket(player, new ConnectionPacket(session.udpSecret(), advertisedHost, udpPort));
    }

    private void runUdpLoop() {
        byte[] buffer = new byte[4096];
        while (running.get()) {
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                udpSocket.receive(datagram);
                byte[] payload = new byte[datagram.getLength()];
                System.arraycopy(datagram.getData(), datagram.getOffset(), payload, 0, datagram.getLength());
                handleUdpDatagram(payload, datagram.getSocketAddress());
            } catch (SocketTimeoutException ignored) {
            } catch (SocketException exception) {
                if (running.get()) {
                    plugin.Logger.warn("Plasmo UDP socket failed: " + exception.getMessage());
                }
            } catch (Exception exception) {
                plugin.Logger.warn("Failed to handle Plasmo UDP packet: " + exception.getMessage());
            }
        }
    }

    private void handleUdpDatagram(byte[] payload, java.net.SocketAddress remoteAddress) throws IOException {
        PlasmoUdpEnvelope envelope = udpCodec.decode(payload);
        if (!sessions.handleUdpPacket(envelope, remoteAddress)) {
            return;
        }

        if (envelope.packet() instanceof PingPacket) {
            sendUdpPacket(new PingPacket(), envelope.secret(), remoteAddress);
            sessions.bySecret(envelope.secret()).ifPresent(session ->
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        Player player = plugin.getServer().getPlayer(session.playerId());
                        if (player == null || !player.isOnline()) {
                            return;
                        }
                        sendTcpPacket(player, new ConfigPacket(serverId, proximityDistance));
                        sendTcpPacket(player, new PlayerListPacket());
                    })
            );
        }
    }

    private void sendUdpPacket(PlasmoPacket packet, UUID secret, java.net.SocketAddress remoteAddress) throws IOException {
        byte[] encoded = udpCodec.encode(packet, secret);
        DatagramPacket datagram = new DatagramPacket(encoded, encoded.length);
        datagram.setSocketAddress(remoteAddress);
        udpSocket.send(datagram);
    }

    private void sendTcpPacket(Player player, PlasmoPacket packet) {
        try {
            player.sendPluginMessage(plugin, PlasmoChannels.MAIN, tcpCodec.encode(packet));
        } catch (Exception exception) {
            plugin.Logger.warn("Failed to send Plasmo packet to " + player.getName() + ": " + exception.getMessage());
        }
    }
}
