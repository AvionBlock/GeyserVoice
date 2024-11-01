package io.greitan.avion.common.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

public class Payloads {
    public static enum PacketType {
        Login, // 0
        Logout, // 1
        Accept, // 2
        Deny, // 3
        Bind, // 4
        Update, // 5
        AckUpdate, // 6
        GetChannels, // 7
        GetChannelSettings, // 8
        SetChannelSettings, // 9
        GetDefaultSettings, // 10
        SetDefaultSettings, // 11

        // Participant Stuff
        GetParticipants, // 12
        DisconnectParticipant, // 13
        GetParticipantBitmask, // 14
        SetParticipantBitmask, // 15
        MuteParticipant, // 16
        UnmuteParticipant, // 17
        DeafenParticipant, // 18
        UndeafenParticipant, // 19

        ANDModParticipantBitmask, // 20
        ORModParticipantBitmask, // 21
        XORModParticipantBitmask, // 22

        ChannelMove; // 23

        public static PacketType fromId(int id) {
            for (PacketType type : PacketType.values()) {
                if (type.ordinal() == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown packet id: " + id);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "PacketId", visible = true)
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
    public static abstract class MCCommPacket {
        public int PacketId;
        public String Token = "";
    }

    public static class LoginPacket extends MCCommPacket {
        public LoginPacket() {
            this.PacketId = PacketType.Login.ordinal();
        }

        public String LoginKey = "";
        public String Version = "";
    }

    public static class LogoutPacket extends MCCommPacket {
        public LogoutPacket() {
            this.PacketId = PacketType.Logout.ordinal();
        }
    }

    public static class AcceptPacket extends MCCommPacket {
        public AcceptPacket() {
            this.PacketId = PacketType.Accept.ordinal();
        }
    }

    public static class DenyPacket extends MCCommPacket {
        public DenyPacket() {
            this.PacketId = PacketType.Deny.ordinal();
        }

        public String Reason = "";
    }

    public static class BindPacket extends MCCommPacket {
        public BindPacket() {
            this.PacketId = PacketType.Bind.ordinal();
        }

        public String PlayerId = "";
        public int PlayerKey = 0;
        public String Gamertag = "";
    }

    public static class UpdatePacket extends MCCommPacket {
        public UpdatePacket() {
            this.PacketId = PacketType.Update.ordinal();
        }

        public List<PlayerData> Players;
    }

    public static class AckUpdatePacket extends MCCommPacket {
        public AckUpdatePacket() {
            this.PacketId = PacketType.AckUpdate.ordinal();
        }

        public List<String> SpeakingPlayers;
    }

    public static class GetChannelsPacket extends MCCommPacket {
        public GetChannelsPacket() {
            this.PacketId = PacketType.GetChannels.ordinal();
        }

        public Map<Integer, ChannelData> Channels;
    }

    public static class GetChannelSettingsPacket extends MCCommPacket {
        public GetChannelSettingsPacket() {
            this.PacketId = PacketType.GetChannelSettings.ordinal();
        }

        public int ChannelId = 0;
        public int ProximityDistance = 30;
        public boolean ProximityToggle = true;
        public boolean VoiceEffects = true;
    }

    public static class SetChannelSettingsPacket extends MCCommPacket {
        public SetChannelSettingsPacket() {
            this.PacketId = PacketType.SetChannelSettings.ordinal();
        }

        public int ChannelId = 0;
        public int ProximityDistance = 30;
        public boolean ProximityToggle = true;
        public boolean VoiceEffects = true;
        public boolean ClearSettings = true;
    }

    public static class GetDefaultSettingsPacket extends MCCommPacket {
        public GetDefaultSettingsPacket() {
            this.PacketId = PacketType.GetDefaultSettings.ordinal();
        }

        public int ProximityDistance = 30;
        public boolean ProximityToggle = true;
        public boolean VoiceEffects = true;
    }

    public static class SetDefaultSettingsPacket extends MCCommPacket {
        public SetDefaultSettingsPacket() {
            this.PacketId = PacketType.SetDefaultSettings.ordinal();
        }

        public int ProximityDistance = 30;
        public boolean ProximityToggle = true;
        public boolean VoiceEffects = true;
    }

    public static class GetParticipantsPacket extends MCCommPacket {
        public GetParticipantsPacket() {
            this.PacketId = PacketType.GetParticipants.ordinal();
        }

        public List<String> Players;
    }

    public static class DisconnectParticipantPacket extends MCCommPacket {
        public DisconnectParticipantPacket() {
            this.PacketId = PacketType.DisconnectParticipant.ordinal();
        }

        public String PlayerId = "";
    }

    public static class PlayerData {
        public String PlayerId = "";
        public String DimensionId = "";
        public LocationData Location = new LocationData();
        public double Rotation = 0.0;
        public double EchoFactor = 0.0;
        public boolean Muffled = false;
        public boolean IsDead = false;

        public PlayerData clone() {
            PlayerData e = new PlayerData();
            e.PlayerId = this.PlayerId;
            e.DimensionId = this.DimensionId;
            e.Location = this.Location;
            e.Rotation = this.Rotation;
            e.EchoFactor = this.EchoFactor;
            e.Muffled = this.Muffled;
            e.IsDead = this.IsDead;
            return e;
        }
    }

    public static class LocationData {
        public double x = 0;
        public double y = 0;
        public double z = 0;
    }

    public static class ChannelData {
        public String Name = "";
        public String Password = "";
        public boolean Locked = false;
        public boolean Hidden = false;
        public ChannelOverrideData OverrideSettings = null;
    }

    public static class ChannelOverrideData {
        public int ProximityDistance = 30;
        public boolean ProximityToggle = true;
        public boolean VoiceEffects = true;
    }
}
