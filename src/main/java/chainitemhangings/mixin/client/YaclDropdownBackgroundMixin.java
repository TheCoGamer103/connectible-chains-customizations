package chainitemhangings.mixin.client;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes YACL's dropdown list readable. YACL draws the list's background with Minecraft's menu
 * background texture, which is see-through (more so with dark-mode resource packs), so the menu
 * behind shows through the suggestions. This paints a solid backing first; YACL's own background
 * and entries are then drawn on top as usual.
 */
@Mixin(value = DropdownWidget.class, remap = false)
public abstract class YaclDropdownBackgroundMixin {

    @Unique private static final int SOLID_BACKING = 0xFF1A1A1E;

    @Shadow protected Dimension<Integer> dropdownDim;

    @Shadow public abstract int dropdownLength();

    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
    private void chainitemhangings$solidBacking(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (dropdownDim == null || dropdownLength() == 0) return;
        graphics.fill(dropdownDim.x(), dropdownDim.y(), dropdownDim.xLimit(), dropdownDim.yLimit(), SOLID_BACKING);
    }
}
