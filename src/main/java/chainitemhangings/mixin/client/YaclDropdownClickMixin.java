package chainitemhangings.mixin.client;

import chainitemhangings.client.DropdownPick;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownWidget;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes clicking an entry in a YACL dropdown that allows any value (YACL 3.9.7).
 *
 * Clicking the list closes the dropdown, and YACL only swaps in the highlighted entry when the typed
 * text is invalid. With "allow any value", typed text is always valid, so the click was ignored and
 * the half-typed text stayed. This puts the clicked (highlighted) entry into the box first; YACL then
 * closes the dropdown and saves it as usual. Clicking outside the list still keeps what you typed.
 */
@Mixin(value = DropdownWidget.class, remap = false)
public abstract class YaclDropdownClickMixin {

    @Shadow @Final private AbstractDropdownControllerElement<?, ?> dropdownElement;
    @Shadow protected int selectedIndex;

    @Shadow public abstract boolean isMouseOver(double mouseX, double mouseY);
    @Shadow public abstract void mouseMoved(double mouseX, double mouseY);

    @Inject(method = "mouseClicked", at = @At("HEAD"), require = 0)
    private void chainitemhangings$pickClickedEntry(MouseButtonEvent event, boolean doubleClick,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (isMouseOver(event.x(), event.y())) {
            // Highlight the row under the click (in case the mouse didn't move first), then use it.
            mouseMoved(event.x(), event.y());
            DropdownPick.apply(dropdownElement, selectedIndex);
        }
    }
}
