package chainitemhangings.mixin.client;

import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Lets the dropdown fix read the suggestions currently shown in the list. */
@Mixin(value = AbstractDropdownControllerElement.class, remap = false)
public interface YaclDropdownValuesAccessor {
    @Accessor(value = "matchingValues", remap = false)
    List<?> chainitemhangings$getMatchingValues();
}
