package io.greitan.avion.common.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Mutable POJO implementation of Network Payloads for compatibility.
 */
public class Payloads {

    public enum PacketType {
        Login, Logout, Accept, Deny, Bind, Update, AckUpdate,
        GetChannels, GetChannelSettings, SetChannelSettings,
        GetDefaultSettings, SetDefaultSettings,
        GetParticipants, DisconnectParticipant,
        GetParticipantBitmask, SetParticipantBitmask,
        MuteParticipant, UnmuteParticipant,
        DeafenParticipant, UndeafenParticipant,
        ANDModParticipantBitmask, ORModParticipantBitmask, XORModParticipantBitmask,
        ChannelMove;

        public static PacketType fromId(int id) {
            if (id < 0 || id >= values().length) {
                throw new IllegalArgumentException("Unknown packet id: " + id);
            }
            return values()[id];
        }
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "PacketId",
        visible = true
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = LoginPacket.class, name = "0"),
        @JsonSubTypes.Type(value = LogoutPacket.class, name = "1"),
        @JsonSubTypes.Type(value = AcceptPacket.class, name = "2"),
        @JsonSubTypes.Type(value = DenyPacket.class, name = "3"),
        @JsonSubTypes.Type(value = BindPacket.class, name = "4"),
        @JsonSubTypes.Type(value = UpdatePacket.class, name = "5"),
        @JsonSubTypes.Type(value = AckUpdatePacket.class, name = "6"),
        @JsonSubTypes.Type(value = GetChannelsPacket.class, name = "7"),
        @JsonSubTypes.Type(value = GetChannelSettingsPacket.class, name = "8"),
        @JsonSubTypes.Type(value = SetChannelSettingsPacket.class, name = "9"),
        @JsonSubTypes.Type(value = GetDefaultSettingsPacket.class, name = "10"),
        @JsonSubTypes.Type(value = SetDefaultSettingsPacket.class, name = "11"),
        @JsonSubTypes.Type(value = GetParticipantsPacket.class, name = "12"),
        @JsonSubTypes.Type(value = DisconnectParticipantPacket.class, name = "13"),
    })
    public interface MCCommPacket {
        int getPacketId();
        String getToken();
    }

    // --- Classes ---

    public static class LoginPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("LoginKey") public String loginKey;
        @JsonProperty("Version") public String version;

        public LoginPacket() {
            this.packetId = PacketType.Login.ordinal();
            this.token = "";
            this.loginKey = "";
            this.version = "";
        }

        public LoginPacket(String loginKey, String version) {
            this.packetId = PacketType.Login.ordinal();
            this.token = "";
            this.loginKey = loginKey;
            this.version = version;
        }
        
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class LogoutPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;

        public LogoutPacket() {
            this.packetId = PacketType.Logout.ordinal();
            this.token = "";
        }

        public LogoutPacket(String token) {
            this.packetId = PacketType.Logout.ordinal();
            this.token = token;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class AcceptPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;

        public AcceptPacket() {
            this.packetId = PacketType.Accept.ordinal();
            this.token = "";
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class DenyPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("Reason") public String reason;

        public DenyPacket() {
            this.packetId = PacketType.Deny.ordinal();
            this.token = "";
            this.reason = "";
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class BindPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("PlayerId") public String playerId;
        @JsonProperty("PlayerKey") public int playerKey;
        @JsonProperty("Gamertag") public String gamertag;

        public BindPacket() {
            this.packetId = PacketType.Bind.ordinal();
            this.token = "";
            this.playerId = "";
            this.playerKey = 0;
            this.gamertag = "";
        }

        public BindPacket(String token, String playerId, int playerKey, String gamertag) {
            this.packetId = PacketType.Bind.ordinal();
            this.token = token;
            this.playerId = playerId;
            this.playerKey = playerKey;
            this.gamertag = gamertag;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class UpdatePacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("Players") public List<PlayerData> players;

        public UpdatePacket() {
            this.packetId = PacketType.Update.ordinal();
            this.token = "";
            this.players = Collections.emptyList();
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class AckUpdatePacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("SpeakingPlayers") public List<String> speakingPlayers;

        public AckUpdatePacket() {
            this.packetId = PacketType.AckUpdate.ordinal();
            this.token = "";
            this.speakingPlayers = Collections.emptyList();
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class GetChannelsPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("Channels") public Map<Integer, ChannelData> channels;

        public GetChannelsPacket() {
            this.packetId = PacketType.GetChannels.ordinal();
            this.token = "";
            this.channels = Collections.emptyMap();
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class GetChannelSettingsPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("ChannelId") public int channelId;
        @JsonProperty("ProximityDistance") public int proximityDistance;
        @JsonProperty("ProximityToggle") public boolean proximityToggle;
        @JsonProperty("VoiceEffects") public boolean voiceEffects;

        public GetChannelSettingsPacket() {
            this.packetId = PacketType.GetChannelSettings.ordinal();
            this.token = "";
            this.channelId = 0;
            this.proximityDistance = 30;
            this.proximityToggle = true;
            this.voiceEffects = true;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class SetChannelSettingsPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("ChannelId") public int channelId;
        @JsonProperty("ProximityDistance") public int proximityDistance;
        @JsonProperty("ProximityToggle") public boolean proximityToggle;
        @JsonProperty("VoiceEffects") public boolean voiceEffects;
        @JsonProperty("ClearSettings") public boolean clearSettings;

        public SetChannelSettingsPacket() {
            this.packetId = PacketType.SetChannelSettings.ordinal();
            this.token = "";
            this.channelId = 0;
            this.proximityDistance = 30;
            this.proximityToggle = true;
            this.voiceEffects = true;
            this.clearSettings = true;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class GetDefaultSettingsPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("ProximityDistance") public int proximityDistance;
        @JsonProperty("ProximityToggle") public boolean proximityToggle;
        @JsonProperty("VoiceEffects") public boolean voiceEffects;

        public GetDefaultSettingsPacket() {
            this.packetId = PacketType.GetDefaultSettings.ordinal();
            this.token = "";
            this.proximityDistance = 30;
            this.proximityToggle = true;
            this.voiceEffects = true;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class SetDefaultSettingsPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("ProximityDistance") public int proximityDistance;
        @JsonProperty("ProximityToggle") public boolean proximityToggle;
        @JsonProperty("VoiceEffects") public boolean voiceEffects;

        public SetDefaultSettingsPacket() {
            this.packetId = PacketType.SetDefaultSettings.ordinal();
            this.token = "";
            this.proximityDistance = 30;
            this.proximityToggle = true;
            this.voiceEffects = true;
        }

        public SetDefaultSettingsPacket(String token, int proximityDistance, boolean proximityToggle, boolean voiceEffects) {
            this.packetId = PacketType.SetDefaultSettings.ordinal();
            this.token = token;
            this.proximityDistance = proximityDistance;
            this.proximityToggle = proximityToggle;
            this.voiceEffects = voiceEffects;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class GetParticipantsPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("Players") public List<String> players;

        public GetParticipantsPacket() {
            this.packetId = PacketType.GetParticipants.ordinal();
            this.token = "";
            this.players = Collections.emptyList();
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    public static class DisconnectParticipantPacket implements MCCommPacket {
        @JsonProperty("PacketId") public int packetId;
        @JsonProperty("Token") public String token;
        @JsonProperty("PlayerId") public String playerId;

        public DisconnectParticipantPacket() {
            this.packetId = PacketType.DisconnectParticipant.ordinal();
            this.token = "";
            this.playerId = "";
        }
        
        public DisconnectParticipantPacket(String token, String playerId) {
            this.packetId = PacketType.DisconnectParticipant.ordinal();
            this.token = token;
            this.playerId = playerId;
        }
        public int getPacketId() { return packetId; }
        public String getToken() { return token; }
    }

    // --- Support Classes ---

    public static class LocationData {
        public double x;
        public double y;
        public double z;

        public LocationData() {}
        public LocationData(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class PlayerData {
        @JsonProperty("PlayerId") public String playerId;
        @JsonProperty("DimensionId") public String dimensionId;
        @JsonProperty("Location") public LocationData location;
        @JsonProperty("Rotation") public double rotation;
        @JsonProperty("EchoFactor") public double echoFactor;
        @JsonProperty("Muffled") public boolean muffled;
        @JsonProperty("IsDead") public boolean isDead;

        public PlayerData() {}
    }

    public static class ChannelOverrideData {
        @JsonProperty("ProximityDistance") public int proximityDistance;
        @JsonProperty("ProximityToggle") public boolean proximityToggle;
        @JsonProperty("VoiceEffects") public boolean voiceEffects;

        public ChannelOverrideData() {}
    }

    public static class ChannelData {
        @JsonProperty("Name") public String name;
        @JsonProperty("Password") public String password;
        @JsonProperty("Locked") public boolean locked;
        @JsonProperty("Hidden") public boolean hidden;
        @JsonProperty("OverrideSettings") public ChannelOverrideData overrideSettings;

        public ChannelData() {}
    }
}
