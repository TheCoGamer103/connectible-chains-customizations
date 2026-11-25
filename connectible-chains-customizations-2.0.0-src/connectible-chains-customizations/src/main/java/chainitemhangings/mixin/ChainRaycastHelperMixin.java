package chainitemhangings.mixin;

import chainitemhangings.config.SyncedConfig;
import chainitemhangings.config.HangingToolConfig;
import chainitemhangings.config.ServerConfig;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the old datapack: anything in either customization list can be hung on a chain,
 * on top of whatever is already in the mod's hangable_items tag (lanterns etc.).
 */
@Mixin(value = ChainRaycastHelper.class, remap = false)
public abstract class ChainRaycastHelperMixin {

    @WrapOperation(method = "tryPlaceHanging",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean chainitemhangings$allowListedItems(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original,
                                                              @Local(argsOnly = true) Player player) {
        if (original.call(stack, tag)) return true;
        if (tag != ModTagRegistry.HANGABLE_ITEMS) return false;
        // The client predicts placement with the copy the server sent it; the server decides for real.
        HangingToolConfig config = player.level().isClientSide() ? SyncedConfig.get() : ServerConfig.get();
        return config.isHangable(stack);
    }
}
