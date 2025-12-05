package io.greitan.avion.common.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Modern Java 21 Records implementation of Network Payloads.
 * Uses Sealed Interfaces for strict type control (SOTA).
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
    public sealed interface MCCommPacket permits 
        LoginPacket, LogoutPacket, AcceptPacket, DenyPacket, BindPacket, 
        UpdatePacket, AckUpdatePacket, GetChannelsPacket, GetChannelSettingsPacket, 
        SetChannelSettingsPacket, GetDefaultSettingsPacket, SetDefaultSettingsPacket, 
        GetParticipantsPacket, DisconnectParticipantPacket {
        
        @JsonProperty("PacketId") int packetId();
        @JsonProperty("Token") String token();
    }

    // --- Records ---

    public record LoginPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("LoginKey") String loginKey,
        @JsonProperty("Version") String version
    ) implements MCCommPacket {
        public LoginPacket(String loginKey, String version) {
            this(PacketType.Login.ordinal(), "", loginKey, version);
        }
        // Default constructor for Jackson
        public LoginPacket() {
            this(PacketType.Login.ordinal(), "", "", "");
        }
    }

    public record LogoutPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token
    ) implements MCCommPacket {
        public LogoutPacket(String token) {
            this(PacketType.Logout.ordinal(), token);
        }
        public LogoutPacket() {
            this(PacketType.Logout.ordinal(), "");
        }
    }

    public record AcceptPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token
    ) implements MCCommPacket {
        public AcceptPacket() {
            this(PacketType.Accept.ordinal(), "");
        }
    }

    public record DenyPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("Reason") String reason
    ) implements MCCommPacket {
        public DenyPacket() {
            this(PacketType.Deny.ordinal(), "", "");
        }
    }

    public record BindPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("PlayerId") String playerId,
        @JsonProperty("PlayerKey") int playerKey,
        @JsonProperty("Gamertag") String gamertag
    ) implements MCCommPacket {
        public BindPacket(String token, String playerId, int playerKey, String gamertag) {
            this(PacketType.Bind.ordinal(), token, playerId, playerKey, gamertag);
        }
        public BindPacket() {
            this(PacketType.Bind.ordinal(), "", "", 0, "");
        }
    }

    public record UpdatePacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("Players") List<PlayerData> players
    ) implements MCCommPacket {
        public UpdatePacket() {
            this(PacketType.Update.ordinal(), "", List.of());
        }
    }

    public record AckUpdatePacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("SpeakingPlayers") List<String> speakingPlayers
    ) implements MCCommPacket {
        public AckUpdatePacket() {
            this(PacketType.AckUpdate.ordinal(), "", List.of());
        }
    }

    public record GetChannelsPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("Channels") Map<Integer, ChannelData> channels
    ) implements MCCommPacket {
        public GetChannelsPacket() {
            this(PacketType.GetChannels.ordinal(), "", Map.of());
        }
    }

    public record GetChannelSettingsPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("ChannelId") int channelId,
        @JsonProperty("ProximityDistance") int proximityDistance,
        @JsonProperty("ProximityToggle") boolean proximityToggle,
        @JsonProperty("VoiceEffects") boolean voiceEffects
    ) implements MCCommPacket {
        public GetChannelSettingsPacket() {
            this(PacketType.GetChannelSettings.ordinal(), "", 0, 30, true, true);
        }
    }

    public record SetChannelSettingsPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("ChannelId") int channelId,
        @JsonProperty("ProximityDistance") int proximityDistance,
        @JsonProperty("ProximityToggle") boolean proximityToggle,
        @JsonProperty("VoiceEffects") boolean voiceEffects,
        @JsonProperty("ClearSettings") boolean clearSettings
    ) implements MCCommPacket {
        public SetChannelSettingsPacket() {
            this(PacketType.SetChannelSettings.ordinal(), "", 0, 30, true, true, true);
        }
    }

    public record GetDefaultSettingsPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("ProximityDistance") int proximityDistance,
        @JsonProperty("ProximityToggle") boolean proximityToggle,
        @JsonProperty("VoiceEffects") boolean voiceEffects
    ) implements MCCommPacket {
        public GetDefaultSettingsPacket() {
            this(PacketType.GetDefaultSettings.ordinal(), "", 30, true, true);
        }
    }

    public record SetDefaultSettingsPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("ProximityDistance") int proximityDistance,
        @JsonProperty("ProximityToggle") boolean proximityToggle,
        @JsonProperty("VoiceEffects") boolean voiceEffects
    ) implements MCCommPacket {
        public SetDefaultSettingsPacket(String token, int proximityDistance, boolean proximityToggle, boolean voiceEffects) {
            this(PacketType.SetDefaultSettings.ordinal(), token, proximityDistance, proximityToggle, voiceEffects);
        }
        public SetDefaultSettingsPacket() {
            this(PacketType.SetDefaultSettings.ordinal(), "", 30, true, true);
        }
    }

    public record GetParticipantsPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("Players") List<String> players
    ) implements MCCommPacket {
        public GetParticipantsPacket() {
            this(PacketType.GetParticipants.ordinal(), "", List.of());
        }
    }

    public record DisconnectParticipantPacket(
        @JsonProperty("PacketId") int packetId,
        @JsonProperty("Token") String token,
        @JsonProperty("PlayerId") String playerId
    ) implements MCCommPacket {
        public DisconnectParticipantPacket(String token, String playerId) {
            this(PacketType.DisconnectParticipant.ordinal(), token, playerId);
        }
        public DisconnectParticipantPacket() {
            this(PacketType.DisconnectParticipant.ordinal(), "", "");
        }
    }

    // --- Support Structures (using plain classes or records as needed) ---

    public record LocationData(
        double x,
        double y,
        double z
    ) {}

    public record PlayerData(
        @JsonProperty("PlayerId") String playerId,
        @JsonProperty("DimensionId") String dimensionId,
        @JsonProperty("Location") LocationData location,
        @JsonProperty("Rotation") double rotation,
        @JsonProperty("EchoFactor") double echoFactor,
        @JsonProperty("Muffled") boolean muffled,
        @JsonProperty("IsDead") boolean isDead
    ) {}

    public record ChannelOverrideData(
        @JsonProperty("ProximityDistance") int proximityDistance,
        @JsonProperty("ProximityToggle") boolean proximityToggle,
        @JsonProperty("VoiceEffects") boolean voiceEffects
    ) {}

    public record ChannelData(
        @JsonProperty("Name") String name,
        @JsonProperty("Password") String password,
        @JsonProperty("Locked") boolean locked,
        @JsonProperty("Hidden") boolean hidden,
        @JsonProperty("OverrideSettings") ChannelOverrideData overrideSettings
    ) {}
}
