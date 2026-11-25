package chainitemhangings.mixin.client;

import chainitemhangings.client.DropdownPick;
import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownWidget;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same fix as YaclDropdownClickMixin, for the keyboard: pressing Enter while the list is open puts the
 * highlighted entry into the box (pick one with the arrow keys or by hovering). If you haven't moved
 * the highlight, it's what you typed, so Enter keeps your own text. Escape still keeps what you typed.
 */
@Mixin(value = AbstractDropdownControllerElement.class, remap = false)
public abstract class YaclDropdownEnterMixin {

    @Shadow protected boolean dropdownVisible;
    @Shadow protected DropdownWidget<?> dropdownWidget;

    @Inject(method = "keyPressed", at = @At("HEAD"), require = 0)
    private void chainitemhangings$pickWithEnter(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int key = event.key();
        if (dropdownVisible && dropdownWidget != null
                && (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER)) {
            DropdownPick.apply((AbstractDropdownControllerElement<?, ?>) (Object) this, dropdownWidget.selectedIndex());
        }
    }
}
