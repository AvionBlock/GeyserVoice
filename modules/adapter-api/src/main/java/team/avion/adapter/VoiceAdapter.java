package team.avion.adapter;

public interface VoiceAdapter extends AutoCloseable {
    String id();

    String displayName();

    AdapterState state();

    void start(AdapterContext context) throws Exception;

    void stop() throws Exception;

    @Override
    default void close() throws Exception {
        stop();
    }
}
