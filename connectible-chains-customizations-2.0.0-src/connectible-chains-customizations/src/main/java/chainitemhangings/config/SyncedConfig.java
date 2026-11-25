package chainitemhangings.config;

/**
 * The client's copy of the config, as last sent by the server. Kept free of client-only
 * classes so shared code can read it safely on a dedicated server too.
 */
public final class SyncedConfig {

    private static volatile HangingToolConfig current = HangingToolConfig.defaults();
    private static volatile boolean serverHasMod = false;
    private static volatile boolean canEdit = false;

    private SyncedConfig() {}

    public static HangingToolConfig get() {
        return current;
    }

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static void receive(HangingToolConfig config, boolean mayEdit) {
        current = config;
        canEdit = mayEdit;
        serverHasMod = true;
    }

    /** Back to built-in defaults when leaving a server. */
    public static void reset() {
        current = HangingToolConfig.defaults();
        canEdit = false;
        serverHasMod = false;
    }
}
