package team.avion.adapter;

public interface VoiceCraftAudioBridge {
    void sendEntityAudio(int entityId, int timestamp, float loudness, byte[] opusFrame);
}
