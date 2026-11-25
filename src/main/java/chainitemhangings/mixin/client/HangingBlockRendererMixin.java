package chainitemhangings.mixin.client;

import chainitemhangings.HangingToolMod;
import chainitemhangings.config.HangingToolConfig;
import chainitemhangings.config.SyncedConfig;
import com.evandev.connectiblechains.util.HangingBlockRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;
import java.util.Set;

/**
 * Reconnectible Chains draws every hanging thing as a block (and items with no block as
 * nothing). This wraps that renderer:
 *  - "Items Displayed as Their Item Texture" (and anything with no block form) are drawn
 *    as their item model, 3D models included.
 *  - "Items Displayed as Their Block" keep the mod's block model, with this mod's scale,
 *    offset and rotation applied. With Offset at 0, nothing is drawn above the chain.
 *
 * Targets Reconnectible Chains 2.4.3 - 2.6.0 for Minecraft 26.3.
 */
@Mixin(value = HangingBlockRenderer.class, remap = false)
public abstract class HangingBlockRendererMixin {

    @Unique private static final Set<Identifier> LOGGED = new HashSet<>();

    @WrapMethod(method = "render")
    private void chainitemhangings$render(Minecraft mc, Identifier id, BlockPos pos, PoseStack matrices,
                                          SubmitNodeCollector collector, float x, float y, Operation<Void> original) {
        HangingToolConfig config = SyncedConfig.get();
        Item item = BuiltInRegistries.ITEM.getValue(id);
        ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        boolean hasBlock = BuiltInRegistries.BLOCK.containsKey(id);

        if (!stack.isEmpty() && (!hasBlock || config.isDisplayedAsItem(stack))) {
            drawItem(mc, id, stack, pos, matrices, collector, x, y, config);
            return;
        }

        HangingToolConfig.ItemSettings s = stack.isEmpty() ? new HangingToolConfig.ItemSettings() : config.settingsFor(id, stack);
        float scale = s.scale > 0 ? s.scale : config.general.blockScale;
        if (scale == 1f && s.offset == 0f && !s.hasRotation()) {
            original.call(mc, id, pos, matrices, collector, x, y);
            return;
        }
        // Turn the block around its own center, then lower it so its highest point sits just under the chain.
        // The mod draws the block filling x-0.5..x+0.5, y-1..y, so its center is (x, y - 0.5).
        float halfHeight = halfHeight(scale, s.rotation);
        matrices.pushPose();
        try {
            matrices.translate(x, y - s.offset - halfHeight, 0f);
            matrices.rotate(rotationOf(s));
            matrices.scale(scale, scale, scale);
            matrices.translate(-x, -(y - 0.5f), 0f);
            original.call(mc, id, pos, matrices, collector, x, y);
        } finally {
            matrices.popPose();
        }
    }

    /** Rotation turns in the flat plane first, then XZ Rotation spins the result around the vertical pole. */
    @Unique
    private static Quaternionf rotationOf(HangingToolConfig.ItemSettings s) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(s.rotationXZ))
                .rotateZ((float) Math.toRadians(-s.rotation));
    }

    /**
     * Half the height of a square of this size after turning it in its plane. Hanging its center this far
     * below the chain puts its highest corner right at the chain, so nothing ever pokes above it.
     * (Spinning around the vertical pole doesn't change the height.)
     */
    @Unique
    private static float halfHeight(float size, float rotationDegrees) {
        double r = Math.toRadians(rotationDegrees);
        return (float) (size * 0.5 * (Math.abs(Math.cos(r)) + Math.abs(Math.sin(r))));
    }

    @Unique
    private static void drawItem(Minecraft mc, Identifier id, ItemStack stack, BlockPos pos, PoseStack matrices,
                                 SubmitNodeCollector collector, float x, float y, HangingToolConfig config) {
        if (!(mc.level instanceof ClientLevel level) || mc.player == null) return;

        HangingToolConfig.ItemSettings s = config.settingsFor(id, stack);
        float scale = s.scale > 0 ? s.scale : config.general.itemScale;
        int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, pos),
                level.getBrightness(LightLayer.SKY, pos));

        matrices.pushPose();
        try {
            // Item models are centred on the origin: lower the center so the highest corner sits under the chain.
            matrices.translate(x, y - halfHeight(scale, s.rotation) - config.general.gap - s.offset, 0f);
            matrices.rotate(rotationOf(s));
            matrices.scale(scale, scale, scale);

            ItemStackRenderState state = new ItemStackRenderState();
            // The resolver reads the world from this entity, so it can't be null.
            mc.getItemModelResolver().updateForNonLiving(state, stack, ItemDisplayContext.FIXED, mc.player);
            state.submit(matrices, collector, light, OverlayTexture.NO_OVERLAY, 0);
        } catch (Exception e) {
            if (LOGGED.add(id)) HangingToolMod.LOGGER.error("Failed to draw hanging item {}", id, e);
        } finally {
            matrices.popPose();
        }
    }
}
