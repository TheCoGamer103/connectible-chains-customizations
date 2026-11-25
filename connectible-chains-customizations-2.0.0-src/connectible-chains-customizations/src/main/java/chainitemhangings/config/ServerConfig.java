package chainitemhangings.config;

import chainitemhangings.HangingToolMod;
import chainitemhangings.net.ConfigSyncS2C;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.nio.file.Files;
import java.nio.file.Path;

/** The server's copy of the config (also used by the built-in singleplayer server). */
public final class ServerConfig {

    public static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("chain_customizations.json");

    private static volatile HangingToolConfig current = HangingToolConfig.defaults();

    private ServerConfig() {}

    public static HangingToolConfig get() {
        return current;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                current = HangingToolConfig.fromJson(Files.readString(FILE));
                HangingToolMod.LOGGER.info("Loaded {}", FILE.getFileName());
            } else {
                current = HangingToolConfig.defaults();
                save();
                HangingToolMod.LOGGER.info("Created {} with default settings", FILE.getFileName());
            }
        } catch (Exception e) {
            HangingToolMod.LOGGER.error("Couldn't read {}; using defaults until it's fixed", FILE, e);
            current = HangingToolConfig.defaults();
        }
    }

    public static void set(HangingToolConfig config) {
        current = config;
        save();
    }

    private static void save() {
        try {
            Files.writeString(FILE, current.toJson());
        } catch (Exception e) {
            HangingToolMod.LOGGER.error("Couldn't save {}", FILE, e);
        }
    }

    /** Operators (permission level 2+) can edit; in singleplayer (not opened to LAN) the player always can. */
    public static boolean canEdit(MinecraftServer server, ServerPlayer player) {
        if (server.isSingleplayer() && !server.isPublished()) return true;
        return player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** Skips players whose client doesn't have the mod; they can still play, they just don't see the extra items. */
    public static void sendTo(MinecraftServer server, ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ConfigSyncS2C.TYPE)) return;
        ServerPlayNetworking.send(player, new ConfigSyncS2C(current.toJson(), canEdit(server, player)));
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : PlayerLookup.all(server)) sendTo(server, player);
    }
}
