package chainitemhangings.mixin.client;

import dev.isxander.yacl3.gui.controllers.ControllerPopupWidget;
import dev.isxander.yacl3.gui.controllers.PopupControllerScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownWidget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes typing in YACL dropdown boxes on Minecraft 26.3 (YACL 3.9.7).
 *
 * In 26.3 a text box only receives typed characters while Minecraft's text input is switched on
 * (Minecraft.onTextInputFocusChange). YACL switches it on when you click a text box, but opening a
 * dropdown moves to a separate popup screen, and switching screens turns text input off again.
 * Nothing switches it back on, so the dropdown's box ignores the keyboard. This turns it on for the
 * popup when it opens (and once more on its first tick), and off again when it closes.
 *
 * Applies to every YACL dropdown, not just this mod's. If a YACL update changes these classes,
 * the fix quietly stops applying instead of crashing (require = 0).
 */
@Mixin(value = PopupControllerScreen.class, remap = false)
public abstract class YaclPopupTextInputMixin {

    @Shadow @Final private ControllerPopupWidget<?> controllerPopup;

    @Unique private boolean chainitemhangings$reEnabled = false;

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void chainitemhangings$enableOnOpen(CallbackInfo ci) {
        chainitemhangings$setTextInput(true);
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void chainitemhangings$enableOnFirstTick(CallbackInfo ci) {
        if (!chainitemhangings$reEnabled) {
            chainitemhangings$reEnabled = true;
            chainitemhangings$setTextInput(true);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), require = 0)
    private void chainitemhangings$disableOnClose(CallbackInfo ci) {
        chainitemhangings$setTextInput(false);
    }

    @Unique
    private void chainitemhangings$setTextInput(boolean on) {
        if (controllerPopup instanceof DropdownWidget<?>) {
            Minecraft.getInstance().onTextInputFocusChange(controllerPopup, on);
        }
    }
}
