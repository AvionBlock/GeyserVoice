package team.avion.protocol;

import team.avion.protocol.McProtocolEnums.CreateEntityResponseCode;
import team.avion.protocol.McProtocolEnums.DestroyEntityResponseCode;
import team.avion.protocol.McProtocolEnums.EffectType;
import team.avion.protocol.McProtocolEnums.McApiPacketType;
import team.avion.protocol.McProtocolEnums.PositioningType;
import team.avion.protocol.McProtocolEnums.ResetResponseCode;
import team.avion.protocol.McProtocolTypes.Guid;
import team.avion.protocol.McProtocolTypes.Vector2;
import team.avion.protocol.McProtocolTypes.Vector3;
import team.avion.protocol.McProtocolTypes.Version;

public final class McPackets {
    private McPackets() {
    }

    public interface NetSerializable {
        void serialize(NetDataWriter writer);

        void deserialize(NetDataReader reader);
    }

    public interface McApiPacket extends NetSerializable {
        McApiPacketType packetType();
    }

    public interface RequestIdPacket {
        String requestId();
    }

    public interface AudioEffect extends NetSerializable {
        EffectType effectType();
    }

    public abstract static class EmptyPacket implements McApiPacket {
        @Override
        public void serialize(NetDataWriter writer) {
        }

        @Override
        public void deserialize(NetDataReader reader) {
        }
    }

    public abstract static class IntValuePacket implements McApiPacket {
        public int Id;
        public int Value;

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putUshort(Value);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Value = reader.getUshort();
        }
    }

    public abstract static class BoolValuePacket implements McApiPacket {
        public int Id;
        public boolean Value;

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putBool(Value);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Value = reader.getBool();
        }
    }

    public abstract static class FloatValuePacket implements McApiPacket {
        public int Id;
        public float Value;

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putFloat(Value);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Value = reader.getFloat();
        }
    }

    public abstract static class StringValuePacket implements McApiPacket {
        public int Id;
        public String Value = "";
        private final int maxLength;

        protected StringValuePacket(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putString(Value, maxLength);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Value = reader.getString(maxLength);
        }
    }

    public abstract static class Vector2ValuePacket implements McApiPacket {
        public int Id;
        public Vector2 Value = new Vector2(0, 0);

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putFloat(Value.x());
            writer.putFloat(Value.y());
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Value = new Vector2(reader.getFloat(), reader.getFloat());
        }
    }

    public abstract static class Vector3ValuePacket implements McApiPacket {
        public int Id;
        public Vector3 Value = new Vector3(0, 0, 0);

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putFloat(Value.x());
            writer.putFloat(Value.y());
            writer.putFloat(Value.z());
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Value = new Vector3(reader.getFloat(), reader.getFloat(), reader.getFloat());
        }
    }

    public abstract static class EntityCreatedPacket implements McApiPacket {
        public int Id;
        public float Loudness;
        public long LastSpoke;
        public String WorldId = "";
        public String Name = "";
        public boolean Muted;
        public boolean Deafened;
        public int TalkBitmask;
        public int ListenBitmask;
        public int EffectBitmask;
        public Vector3 Position = new Vector3(0, 0, 0);
        public Vector2 Rotation = new Vector2(0, 0);
        public float CaveFactor;
        public float MuffleFactor;

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putFloat(Loudness);
            writer.putLong(LastSpoke);
            writer.putString(WorldId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(Name, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putBool(Muted);
            writer.putBool(Deafened);
            writer.putUshort(TalkBitmask);
            writer.putUshort(ListenBitmask);
            writer.putUshort(EffectBitmask);
            writer.putFloat(Position.x());
            writer.putFloat(Position.y());
            writer.putFloat(Position.z());
            writer.putFloat(Rotation.x());
            writer.putFloat(Rotation.y());
            writer.putFloat(CaveFactor);
            writer.putFloat(MuffleFactor);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Loudness = reader.getFloat();
            LastSpoke = reader.getLong();
            WorldId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Name = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Muted = reader.getBool();
            Deafened = reader.getBool();
            TalkBitmask = reader.getUshort();
            ListenBitmask = reader.getUshort();
            EffectBitmask = reader.getUshort();
            Position = new Vector3(reader.getFloat(), reader.getFloat(), reader.getFloat());
            Rotation = new Vector2(reader.getFloat(), reader.getFloat());
            CaveFactor = reader.getFloat();
            MuffleFactor = reader.getFloat();
        }
    }

    public static final class VisibilityEffect implements AudioEffect {
        @Override
        public EffectType effectType() {
            return EffectType.VISIBILITY;
        }

        @Override
        public void serialize(NetDataWriter writer) {
        }

        @Override
        public void deserialize(NetDataReader reader) {
        }
    }

    public static final class ProximityEffect implements AudioEffect {
        public float MinRange;
        public float MaxRange;
        public float WetDry = 1.0f;

        @Override
        public EffectType effectType() {
            return EffectType.PROXIMITY;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putFloat(MinRange);
            writer.putFloat(MaxRange);
            writer.putFloat(WetDry);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            MinRange = reader.getFloat();
            MaxRange = reader.getFloat();
            WetDry = reader.getFloat();
        }
    }

    public static final class DirectionalEffect implements AudioEffect {
        public float WetDry = 1.0f;

        @Override
        public EffectType effectType() {
            return EffectType.DIRECTIONAL;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putFloat(WetDry);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            WetDry = reader.getFloat();
        }
    }

    public static final class ProximityEchoEffect implements AudioEffect {
        public float Delay = 0.5f;
        public float Range;
        public float WetDry = 1.0f;

        @Override
        public EffectType effectType() {
            return EffectType.PROXIMITY_ECHO;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putFloat(Delay);
            writer.putFloat(Range);
            writer.putFloat(WetDry);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Delay = reader.getFloat();
            Range = reader.getFloat();
            WetDry = reader.getFloat();
        }
    }

    public static final class EchoEffect implements AudioEffect {
        public float Delay = 0.5f;
        public float Feedback = 0.5f;
        public float WetDry = 1.0f;

        @Override
        public EffectType effectType() {
            return EffectType.ECHO;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putFloat(Delay);
            writer.putFloat(Feedback);
            writer.putFloat(WetDry);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Delay = reader.getFloat();
            Feedback = reader.getFloat();
            WetDry = reader.getFloat();
        }
    }

    public static final class ProximityMuffleEffect implements AudioEffect {
        public float WetDry = 1.0f;

        @Override
        public EffectType effectType() {
            return EffectType.PROXIMITY_MUFFLE;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putFloat(WetDry);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            WetDry = reader.getFloat();
        }
    }

    public static final class MuffleEffect implements AudioEffect {
        public float WetDry = 1.0f;

        @Override
        public EffectType effectType() {
            return EffectType.MUFFLE;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putFloat(WetDry);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            WetDry = reader.getFloat();
        }
    }

    public static final class LoginRequestPacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public String Token = "";
        public Version Version = VoiceCraftProtocol.VERSION;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.LOGIN_REQUEST;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(Token, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putInt(Version.major());
            writer.putInt(Version.minor());
            writer.putInt(Version.build());
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Token = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Version = new Version(reader.getInt(), reader.getInt(), reader.getInt());
        }
    }

    public static final class LogoutRequestPacket implements McApiPacket {
        public String Token = "";

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.LOGOUT_REQUEST;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(Token, McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Token = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
        }
    }

    public static final class PingRequestPacket extends EmptyPacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.PING_REQUEST;
        }
    }

    public static final class AcceptResponsePacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public String Token = "";

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ACCEPT_RESPONSE;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(Token, McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Token = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
        }
    }

    public static final class DenyResponsePacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public String Reason = "";

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.DENY_RESPONSE;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(Reason, McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Reason = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
        }
    }

    public static final class PingResponsePacket implements McApiPacket {
        public String Token = "";

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.PING_RESPONSE;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(Token, McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Token = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
        }
    }

    public static final class ResetRequestPacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.RESET_REQUEST;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
        }
    }

    public static final class SetEffectRequestPacket implements McApiPacket {
        public int Bitmask;
        public EffectType EffectTypeValue = EffectType.NONE;
        public AudioEffect Effect;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_EFFECT_REQUEST;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putUshort(Bitmask);
            writer.putByte((Effect != null ? Effect.effectType() : EffectTypeValue).ordinal());
            if (Effect != null) {
                Effect.serialize(writer);
            }
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Bitmask = reader.getUshort();
            EffectTypeValue = McProtocolEnums.EffectType.fromByte(reader.getByte());
            Effect = McProtocolCodec.readEffect(EffectTypeValue, reader);
        }
    }

    public static final class ClearEffectsRequestPacket extends EmptyPacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.CLEAR_EFFECTS_REQUEST;
        }
    }

    public static final class CreateEntityRequestPacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public String WorldId = "";
        public String Name = "";
        public boolean Muted;
        public boolean Deafened;
        public int TalkBitmask;
        public int ListenBitmask;
        public int EffectBitmask;
        public Vector3 Position = new Vector3(0, 0, 0);
        public Vector2 Rotation = new Vector2(0, 0);
        public float CaveFactor;
        public float MuffleFactor;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.CREATE_ENTITY_REQUEST;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(WorldId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(Name, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putBool(Muted);
            writer.putBool(Deafened);
            writer.putUshort(TalkBitmask);
            writer.putUshort(ListenBitmask);
            writer.putUshort(EffectBitmask);
            writer.putFloat(Position.x());
            writer.putFloat(Position.y());
            writer.putFloat(Position.z());
            writer.putFloat(Rotation.x());
            writer.putFloat(Rotation.y());
            writer.putFloat(CaveFactor);
            writer.putFloat(MuffleFactor);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            WorldId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Name = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Muted = reader.getBool();
            Deafened = reader.getBool();
            TalkBitmask = reader.getUshort();
            ListenBitmask = reader.getUshort();
            EffectBitmask = reader.getUshort();
            Position = new Vector3(reader.getFloat(), reader.getFloat(), reader.getFloat());
            Rotation = new Vector2(reader.getFloat(), reader.getFloat());
            CaveFactor = reader.getFloat();
            MuffleFactor = reader.getFloat();
        }
    }

    public static final class DestroyEntityRequestPacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public int Id;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.DESTROY_ENTITY_REQUEST;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putInt(Id);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            Id = reader.getInt();
        }
    }

    public static final class EntityAudioRequestPacket implements McApiPacket {
        public int Id;
        public int Timestamp;
        public float FrameLoudness;
        public int Length;
        public byte[] Data = new byte[0];

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ENTITY_AUDIO_REQUEST;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putUshort(Timestamp);
            writer.putFloat(FrameLoudness);
            writer.putBytes(Data, 0, Length);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Timestamp = reader.getUshort();
            FrameLoudness = reader.getFloat();
            Length = reader.availableBytes();
            if (Length > McProtocolConstants.MAXIMUM_ENCODED_BYTES) {
                throw new IllegalStateException("Array length exceeds maximum number of bytes per packet");
            }
            Data = new byte[Length];
            reader.getBytes(Data, Length);
        }
    }

    public static final class SetEntityTitleRequestPacket extends StringValuePacket {
        public SetEntityTitleRequestPacket() {
            super(McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_TITLE_REQUEST;
        }
    }

    public static final class SetEntityDescriptionRequestPacket extends StringValuePacket {
        public SetEntityDescriptionRequestPacket() {
            super(McProtocolConstants.MAX_DESCRIPTION_STRING_LENGTH);
        }

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_DESCRIPTION_REQUEST;
        }
    }

    public static final class SetEntityWorldIdRequestPacket extends StringValuePacket {
        public SetEntityWorldIdRequestPacket() {
            super(McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_WORLD_ID_REQUEST;
        }
    }

    public static final class SetEntityNameRequestPacket extends StringValuePacket {
        public SetEntityNameRequestPacket() {
            super(McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_NAME_REQUEST;
        }
    }

    public static final class SetEntityMuteRequestPacket extends BoolValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_MUTE_REQUEST;
        }
    }

    public static final class SetEntityDeafenRequestPacket extends BoolValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_DEAFEN_REQUEST;
        }
    }

    public static final class SetEntityTalkBitmaskRequestPacket extends IntValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_TALK_BITMASK_REQUEST;
        }
    }

    public static final class SetEntityListenBitmaskRequestPacket extends IntValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_LISTEN_BITMASK_REQUEST;
        }
    }

    public static final class SetEntityEffectBitmaskRequestPacket extends IntValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_EFFECT_BITMASK_REQUEST;
        }
    }

    public static final class SetEntityPositionRequestPacket extends Vector3ValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_POSITION_REQUEST;
        }
    }

    public static final class SetEntityRotationRequestPacket extends Vector2ValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_ROTATION_REQUEST;
        }
    }

    public static final class SetEntityCaveFactorRequestPacket extends FloatValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_CAVE_FACTOR_REQUEST;
        }
    }

    public static final class SetEntityMuffleFactorRequestPacket extends FloatValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.SET_ENTITY_MUFFLE_FACTOR_REQUEST;
        }
    }

    public static final class ResetResponsePacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public ResetResponseCode ResponseCode = ResetResponseCode.OK;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.RESET_RESPONSE;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putSbyte(ResponseCode.code());
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            ResponseCode = ResetResponseCode.fromCode(reader.getSbyte());
        }
    }

    public static final class CreateEntityResponsePacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public CreateEntityResponseCode ResponseCode = CreateEntityResponseCode.OK;
        public int Id;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.CREATE_ENTITY_RESPONSE;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putSbyte(ResponseCode.code());
            writer.putInt(Id);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            ResponseCode = CreateEntityResponseCode.fromCode(reader.getSbyte());
            Id = reader.getInt();
        }
    }

    public static final class DestroyEntityResponsePacket implements McApiPacket, RequestIdPacket {
        public String RequestId = "";
        public DestroyEntityResponseCode ResponseCode = DestroyEntityResponseCode.OK;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.DESTROY_ENTITY_RESPONSE;
        }

        @Override
        public String requestId() {
            return RequestId;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putString(RequestId, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putSbyte(ResponseCode.code());
        }

        @Override
        public void deserialize(NetDataReader reader) {
            RequestId = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            ResponseCode = DestroyEntityResponseCode.fromCode(reader.getSbyte());
        }
    }

    public static final class OnEffectUpdatedPacket implements McApiPacket {
        public int Bitmask;
        public EffectType EffectTypeValue = EffectType.NONE;
        public AudioEffect Effect;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_EFFECT_UPDATED;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putUshort(Bitmask);
            writer.putByte((Effect != null ? Effect.effectType() : EffectTypeValue).ordinal());
            if (Effect != null) {
                Effect.serialize(writer);
            }
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Bitmask = reader.getUshort();
            EffectTypeValue = McProtocolEnums.EffectType.fromByte(reader.getByte());
            Effect = McProtocolCodec.readEffect(EffectTypeValue, reader);
        }
    }

    public static final class OnEntityCreatedPacket extends EntityCreatedPacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_CREATED;
        }
    }

    public static final class OnNetworkEntityCreatedPacket extends EntityCreatedPacket {
        public Guid UserGuid = Guid.empty();
        public Guid ServerUserGuid = Guid.empty();
        public String Locale = "";
        public PositioningType PositioningTypeValue = PositioningType.SERVER;
        public boolean ServerMuted;
        public boolean ServerDeafened;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_NETWORK_ENTITY_CREATED;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            super.serialize(writer);
            writer.putString(UserGuid.toString(), McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(ServerUserGuid.toString(), McProtocolConstants.MAX_STRING_LENGTH);
            writer.putString(Locale, McProtocolConstants.MAX_STRING_LENGTH);
            writer.putByte(PositioningTypeValue.ordinal());
            writer.putBool(ServerMuted);
            writer.putBool(ServerDeafened);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            super.deserialize(reader);
            UserGuid = Guid.parse(reader.getString(McProtocolConstants.MAX_STRING_LENGTH));
            ServerUserGuid = Guid.parse(reader.getString(McProtocolConstants.MAX_STRING_LENGTH));
            Locale = reader.getString(McProtocolConstants.MAX_STRING_LENGTH);
            PositioningTypeValue = McProtocolEnums.PositioningType.fromByte(reader.getByte());
            ServerMuted = reader.getBool();
            ServerDeafened = reader.getBool();
        }
    }

    public static final class OnEntityDestroyedPacket implements McApiPacket {
        public int Id;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_DESTROYED;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
        }
    }

    public static final class OnEntityVisibilityUpdatedPacket implements McApiPacket {
        public int Id;
        public int Id2;
        public boolean Value;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_VISIBILITY_UPDATED;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putInt(Id2);
            writer.putBool(Value);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Id2 = reader.getInt();
            Value = reader.getBool();
        }
    }

    public static final class OnEntityWorldIdUpdatedPacket extends StringValuePacket {
        public OnEntityWorldIdUpdatedPacket() {
            super(McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_WORLD_ID_UPDATED;
        }
    }

    public static final class OnEntityNameUpdatedPacket extends StringValuePacket {
        public OnEntityNameUpdatedPacket() {
            super(McProtocolConstants.MAX_STRING_LENGTH);
        }

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_NAME_UPDATED;
        }
    }

    public static final class OnEntityMuteUpdatedPacket extends BoolValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_MUTE_UPDATED;
        }
    }

    public static final class OnEntityDeafenUpdatedPacket extends BoolValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_DEAFEN_UPDATED;
        }
    }

    public static final class OnEntityServerMuteUpdatedPacket extends BoolValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_SERVER_MUTE_UPDATED;
        }
    }

    public static final class OnEntityServerDeafenUpdatedPacket extends BoolValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_SERVER_DEAFEN_UPDATED;
        }
    }

    public static final class OnEntityTalkBitmaskUpdatedPacket extends IntValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_TALK_BITMASK_UPDATED;
        }
    }

    public static final class OnEntityListenBitmaskUpdatedPacket extends IntValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_LISTEN_BITMASK_UPDATED;
        }
    }

    public static final class OnEntityEffectBitmaskUpdatedPacket extends IntValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_EFFECT_BITMASK_UPDATED;
        }
    }

    public static final class OnEntityPositionUpdatedPacket extends Vector3ValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_POSITION_UPDATED;
        }
    }

    public static final class OnEntityRotationUpdatedPacket extends Vector2ValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_ROTATION_UPDATED;
        }
    }

    public static final class OnEntityCaveFactorUpdatedPacket extends FloatValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_CAVE_FACTOR_UPDATED;
        }
    }

    public static final class OnEntityMuffleFactorUpdatedPacket extends FloatValuePacket {
        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_MUFFLE_FACTOR_UPDATED;
        }
    }

    public static final class OnEntityAudioReceivedPacket implements McApiPacket {
        public int Id;
        public int Timestamp;
        public float FrameLoudness;

        @Override
        public McApiPacketType packetType() {
            return McApiPacketType.ON_ENTITY_AUDIO_RECEIVED;
        }

        @Override
        public void serialize(NetDataWriter writer) {
            writer.putInt(Id);
            writer.putUshort(Timestamp);
            writer.putFloat(FrameLoudness);
        }

        @Override
        public void deserialize(NetDataReader reader) {
            Id = reader.getInt();
            Timestamp = reader.getUshort();
            FrameLoudness = reader.getFloat();
        }
    }
}
