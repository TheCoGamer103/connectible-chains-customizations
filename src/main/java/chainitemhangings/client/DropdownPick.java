package chainitemhangings.client;

import chainitemhangings.mixin.client.YaclDropdownValuesAccessor;
import chainitemhangings.mixin.client.YaclStringFieldAccessor;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;

import java.util.List;

/** Puts the dropdown entry at {@code index} into the dropdown's text box. */
public final class DropdownPick {

    private DropdownPick() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void apply(AbstractDropdownControllerElement element, int index) {
        if (element == null) return;
        List<?> values = ((YaclDropdownValuesAccessor) (Object) element).chainitemhangings$getMatchingValues();
        if (values == null || index < 0 || index >= values.size()) return;
        String picked = element.getString(values.get(index));
        if (picked == null) return;
        YaclStringFieldAccessor field = (YaclStringFieldAccessor) (Object) element;
        field.chainitemhangings$setInputField(picked);
        field.chainitemhangings$setCaretPos(picked.length());
    }
}
