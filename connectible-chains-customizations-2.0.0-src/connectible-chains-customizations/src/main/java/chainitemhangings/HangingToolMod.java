package chainitemhangings;

import chainitemhangings.config.HangingToolConfig;
import chainitemhangings.config.ServerConfig;
import chainitemhangings.net.ConfigSyncS2C;
import chainitemhangings.net.ConfigUpdateC2S;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HangingToolMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Chain Customizations");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncS2C.TYPE, ConfigSyncS2C.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConfigUpdateC2S.TYPE, ConfigUpdateC2S.STREAM_CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.load());
        // After /reload, tags may have changed, so tag entries are matched again.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> ServerConfig.get().clearCaches());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerConfig.sendTo(server, handler.player));

        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2S.TYPE, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            server.execute(() -> handleUpdate(server, player, payload.json()));
        });

        LOGGER.info("Connectible Chains: Customizations loaded");
    }

    private static void handleUpdate(MinecraftServer server, ServerPlayer player, String json) {
        if (!ServerConfig.canEdit(server, player)) {
            player.sendSystemMessage(Component.literal("Only operators can change chain customization settings on this server."));
            ServerConfig.sendTo(server, player); // put their copy back in sync
            return;
        }
        HangingToolConfig config;
        try {
            config = HangingToolConfig.fromJson(json);
        } catch (Exception e) {
            LOGGER.warn("Ignored a broken chain customizations config from {}", player.getName().getString(), e);
            return;
        }
        ServerConfig.set(config);
        LOGGER.info("{} updated the chain customization settings", player.getName().getString());
        ServerConfig.sendToAll(server);
    }
}
