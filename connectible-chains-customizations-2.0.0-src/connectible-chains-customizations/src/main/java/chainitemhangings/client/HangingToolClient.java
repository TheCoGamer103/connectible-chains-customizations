package chainitemhangings.client;

import chainitemhangings.HangingToolMod;
import chainitemhangings.config.HangingToolConfig;
import chainitemhangings.config.SyncedConfig;
import chainitemhangings.net.ConfigSyncS2C;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class HangingToolClient implements ClientModInitializer {

    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    try {
                        SyncedConfig.receive(HangingToolConfig.fromJson(payload.json()), payload.canEdit());
                    } catch (Exception e) {
                        HangingToolMod.LOGGER.error("Couldn't read the chain customization settings sent by the server", e);
                    }
                }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SyncedConfig.reset());

        // Unbound by default; set it in Options > Controls > Key Binds.
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chainitemhangings.open_menu", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) tryOpenMenu(client);
        });
    }

    private static void tryOpenMenu(Minecraft mc) {
        if (mc.player == null) return;
        // The key only works in-game with no other screen open, so the menu needs no parent screen.
        if (!SyncedConfig.serverHasMod()) {
            mc.gui.setScreen(HangingToolScreen.notice(
                    "Connectible Chains: Customizations isn't installed on this server, so its settings can't be changed here."));
        } else if (!SyncedConfig.canEdit()) {
            mc.gui.setScreen(HangingToolScreen.notice(
                    "Only operators can change chain customization settings on this server."));
        } else {
            mc.gui.setScreen(HangingToolScreen.create(null, SyncedConfig.get().copy()));
        }
    }
}
