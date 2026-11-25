package chainitemhangings.mixin.client;

import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the dropdown fix put the picked entry into YACL's text box. */
@Mixin(value = StringControllerElement.class, remap = false)
public interface YaclStringFieldAccessor {
    @Accessor(value = "inputField", remap = false)
    void chainitemhangings$setInputField(String value);

    @Accessor(value = "caretPos", remap = false)
    void chainitemhangings$setCaretPos(int pos);
}
