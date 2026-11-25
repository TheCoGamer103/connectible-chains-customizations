package chainitemhangings.client;

import chainitemhangings.config.HangingToolConfig;
import chainitemhangings.config.HangingToolConfig.ItemSettings;
import chainitemhangings.net.ConfigUpdateC2S;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The in-game Chain Customizations menu. Edits a copy; Save sends it to the server, which checks permission. */
public final class HangingToolScreen {

    private static final List<String> COMMON_PATTERNS = List.of(
            "*_wall", "*_fence", "*_fence_gate", "*_door", "*_trapdoor", "*_slab", "*_stairs",
            "*_button", "*_pressure_plate", "*_sign", "*_hanging_sign", "*_banner", "*_carpet",
            "*_candle", "*_lantern", "*_chain", "*_rod", "*lightning_rod", "*_glass_pane", "*_bars",
            "*_sword", "*_shovel", "*_pickaxe", "*_axe", "*_hoe", "*_spear");

    private HangingToolScreen() {}

    public static Screen create(Screen parent, HangingToolConfig working) {
        HangingToolConfig defaults = HangingToolConfig.defaults();
        List<String> suggestions = suggestions();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Connectible Chains: Customizations"))
                .save(() -> {
                    working.perItem.values().removeIf(ItemSettings::isDefault);
                    ClientPlayNetworking.send(new ConfigUpdateC2S(working.toJson()));
                });

        // ------------------------------------------------------------ General
        ConfigCategory.Builder general = ConfigCategory.createBuilder()
                .name(Component.literal("General"));
        general.option(LabelOption.create(Component.literal(
                "Changes are saved to the server and apply for everyone. Only operators can save on a server.")));
        general.option(floatOption("Default Item Texture Scale",
                "Size of items drawn as their item texture when they have no scale of their own. 0.5 is item-frame size.",
                defaults.general.itemScale, () -> working.general.itemScale, v -> working.general.itemScale = v));
        general.option(floatOption("Default Block Scale",
                "Size of items drawn as their block when they have no scale of their own. 1 is normal block size.",
                defaults.general.blockScale, () -> working.general.blockScale, v -> working.general.blockScale = v));
        general.option(floatOption("Gap Below Chain",
                "Space between the chain and the top of items drawn as their item texture, in blocks.",
                defaults.general.gap, () -> working.general.gap, v -> working.general.gap = v));

        // ------------------------------------------------------------ the two lists
        ConfigCategory asBlock = listCategory("Items Displayed as Their Block",
                "Hang on chains drawn as their block model.",
                defaults.itemsAsBlock, () -> working.itemsAsBlock, v -> working.itemsAsBlock = cleaned(v), suggestions);
        ConfigCategory asItem = listCategory("Items Displayed as Their Item Texture",
                "Hang on chains drawn as their item, like in an item frame. 3D item models are used when an item has one.",
                defaults.itemsAsItem, () -> working.itemsAsItem, v -> working.itemsAsItem = cleaned(v), suggestions);

        // ------------------------------------------------------------ Per-item Settings
        ConfigCategory.Builder perItem = ConfigCategory.createBuilder()
                .name(Component.literal("Per-item Settings"));
        perItem.option(LabelOption.create(Component.literal(
                "One group per entry in the two lists. Added something? Save, close and reopen the menu to see its group here. "
                        + "An exact item ID beats a * wildcard, and a wildcard beats a #tag.")));

        Set<String> entries = new LinkedHashSet<>(working.itemsAsItem);
        entries.addAll(working.itemsAsBlock);
        for (String entry : entries) {
            ItemSettings def = defaults.perItem.getOrDefault(entry, new ItemSettings());
            Supplier<ItemSettings> s = () -> working.perItem.computeIfAbsent(entry, k -> new ItemSettings());
            String kind = working.itemsAsItem.contains(entry) ? "item texture" : "block";
            perItem.group(OptionGroup.createBuilder()
                    .name(Component.literal(entry))
                    .description(OptionDescription.of(Component.literal("Drawn as its " + kind + ".")))
                    .collapsed(true)
                    .option(floatOption("Scale", "0 uses the default scale from General.",
                            def.scale, () -> s.get().scale, v -> s.get().scale = Math.max(0f, v)))
                    .option(floatOption("Offset", "Extra drop below the chain, in blocks. Negative raises it; far enough and it goes above the chain.",
                            def.offset, () -> s.get().offset, v -> s.get().offset = v))
                    .option(floatOption("Rotation", "Turn in the item's own flat plane, in degrees. Flip the sign to turn the other way. With Offset at 0 it's lowered so no corner rises above the chain.",
                            def.rotation, () -> s.get().rotation, v -> s.get().rotation = v))
                    .option(floatOption("XZ Rotation", "Spin around a pole running top to bottom through its center, in degrees. Doesn't change how high it hangs.",
                            def.rotationXZ, () -> s.get().rotationXZ, v -> s.get().rotationXZ = v))
                    .build());
        }

        return builder
                .category(general.build())
                .category(asBlock)
                .category(asItem)
                .category(perItem.build())
                .build()
                .generateScreen(parent);
    }

    /** A small screen with just a message, for when the menu can't be used here. */
    public static Screen notice(String message) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Connectible Chains: Customizations"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Info"))
                        .option(LabelOption.create(Component.literal(message)))
                        .build())
                .save(() -> {})
                .build()
                .generateScreen(null);
    }

    private static ConfigCategory listCategory(String name, String what, List<String> def,
                                               Supplier<List<String>> getter, Consumer<List<String>> setter,
                                               List<String> suggestions) {
        return ConfigCategory.createBuilder()
                .name(Component.literal(name))
                .group(ListOption.<String>createBuilder()
                        .name(Component.literal(name))
                        .description(OptionDescription.of(Component.literal(what + "\n\n"
                                + "Type to search. Entries can be an item ID (minecraft:stick), a * wildcard for a whole group "
                                + "(*_door, mymod:*_wall), which also catches modded variants, or a #tag (#minecraft:doors). "
                                + "List one member of a group on its own to give it its own settings.")))
                        .binding(def, getter, setter)
                        .controller(opt -> DropdownStringControllerBuilder.create(opt)
                                .values(suggestions)
                                .allowAnyValue(true)
                                .allowEmptyValue(true))
                        .initial("")
                        .collapsed(false)
                        .build())
                .build();
    }

    private static Option<Float> floatOption(String name, String tooltip, float def,
                                             Supplier<Float> getter, Consumer<Float> setter) {
        return Option.<Float>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(tooltip)))
                .binding(def, getter, setter)
                .controller(FloatFieldControllerBuilder::create)
                .build();
    }

    /** Drops blank and invalid entries (an empty new row, a typo) and duplicates. */
    private static List<String> cleaned(List<String> list) {
        List<String> out = new ArrayList<>();
        for (String s : list) {
            String t = s == null ? "" : s.trim();
            if (HangingToolConfig.isValidEntry(t) && !out.contains(t)) out.add(t);
        }
        return out;
    }

    /** Every item ID, every item tag (as #tag) and some handy * patterns, for search-as-you-type. */
    private static List<String> suggestions() {
        // Wildcards first: they catch modded variants even when a mod doesn't tag its items.
        List<String> out = new ArrayList<>(COMMON_PATTERNS);
        BuiltInRegistries.ITEM.getTags()
                .map(named -> "#" + named.key().location())
                .sorted()
                .forEach(out::add);
        BuiltInRegistries.ITEM.keySet().stream()
                .map(Object::toString)
                .sorted()
                .forEach(out::add);
        return out;
    }
}
