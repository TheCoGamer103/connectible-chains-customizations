package chainitemhangings.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Everything in config/chain_customizations.json. The server owns this file and sends it to
 * every player; in singleplayer the built-in server reads the same file.
 *
 * Entries can be:
 *   an item ID        minecraft:stick
 *   a * pattern       *_door  or  mymod:*_wall   (a wildcard group; catches modded variants too)
 *   an item tag       #minecraft:doors          (only what mods add to the tag)
 * An exact item ID always beats a pattern, and a pattern beats a tag, so one member of a
 * group can be listed on its own with its own settings (or even in the other list).
 */
public final class HangingToolConfig {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @SerializedName("General")
    public General general = new General();

    @SerializedName("Items Displayed as Their Block")
    public List<String> itemsAsBlock = new ArrayList<>();

    @SerializedName("Items Displayed as Their Item Texture")
    public List<String> itemsAsItem = new ArrayList<>();

    @SerializedName("Per-item Settings")
    public Map<String, ItemSettings> perItem = new LinkedHashMap<>();

    public static final class General {
        @SerializedName("Default Item Texture Scale")
        public float itemScale = 0.75f;
        @SerializedName("Default Block Scale")
        public float blockScale = 1.0f;
        @SerializedName("Gap Below Chain")
        public float gap = 0.02f;
    }

    public static final class ItemSettings {
        /** 0 means "use the default scale from General". */
        @SerializedName("Scale")
        public float scale = 0f;
        /** Extra drop below the chain, in blocks. Negative raises it (and can lift it above the chain). */
        @SerializedName("Offset")
        public float offset = 0f;
        /** Turn in the item's own plane, in degrees. Flip the sign to turn the other way. */
        @SerializedName("Rotation")
        public float rotation = 0f;
        /** Spin around a pole running top to bottom, in degrees. Doesn't change how high it hangs. */
        @SerializedName("XZ Rotation")
        public float rotationXZ = 0f;

        public ItemSettings() {}

        public ItemSettings(float scale, float offset, float rotation) {
            this.scale = scale;
            this.offset = offset;
            this.rotation = rotation;
        }

        public boolean isDefault() {
            return scale == 0f && offset == 0f && rotation == 0f && rotationXZ == 0f;
        }

        public boolean hasRotation() {
            return rotation != 0f || rotationXZ != 0f;
        }
    }

    /**
     * The settings Craig tuned in 1.0.x, used when no file exists yet. Big groups use * patterns rather
     * than tags so modded variants are caught even when their mod doesn't tag them.
     */
    public static HangingToolConfig defaults() {
        HangingToolConfig c = new HangingToolConfig();

        c.itemsAsBlock.addAll(List.of("*_fence", "*_wall", "*lightning_rod", "minecraft:end_rod"));

        List<String> handles = List.of("*_shovel", "*_spear",
                "minecraft:stick", "minecraft:blaze_rod", "minecraft:breeze_rod", "minecraft:fishing_rod");
        c.itemsAsItem.addAll(handles);
        c.itemsAsItem.addAll(List.of("minecraft:echo_shard", "minecraft:lever", "minecraft:tripwire_hook", "minecraft:lead"));

        for (String h : handles) c.perItem.put(h, new ItemSettings(0f, 0f, -135f));
        c.perItem.put("minecraft:echo_shard", new ItemSettings(0f, 0f, 135f));
        for (String f : List.of("minecraft:lever", "minecraft:tripwire_hook", "minecraft:lead")) {
            c.perItem.put(f, new ItemSettings(0f, 0f, 180f));
        }
        return c;
    }

    // ---------------------------------------------------------------- JSON

    public String toJson() {
        return GSON.toJson(this);
    }

    /** Parses JSON; missing sections get defaults, bad entries are dropped. Never returns null. */
    public static HangingToolConfig fromJson(String json) {
        HangingToolConfig c = GSON.fromJson(json, HangingToolConfig.class);
        if (c == null) return defaults();
        if (c.general == null) c.general = new General();
        c.itemsAsBlock = clean(c.itemsAsBlock);
        c.itemsAsItem = clean(c.itemsAsItem);
        if (c.perItem == null) c.perItem = new LinkedHashMap<>();
        c.perItem.entrySet().removeIf(e -> e.getValue() == null || !isValidEntry(e.getKey()));
        for (ItemSettings s : c.perItem.values()) {
            s.scale = Math.max(0f, s.scale);
        }
        return c;
    }

    private static List<String> clean(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) {
            if (s == null) continue;
            String t = s.trim();
            if (isValidEntry(t) && !out.contains(t)) out.add(t);
        }
        return out;
    }

    public HangingToolConfig copy() {
        return fromJson(toJson());
    }

    // ---------------------------------------------------------------- entries

    public static boolean isPattern(String entry) {
        return entry.contains("*");
    }

    public static boolean isTag(String entry) {
        return entry.startsWith("#");
    }

    public static boolean isValidEntry(String entry) {
        if (entry == null || entry.isBlank()) return false;
        if (isPattern(entry)) return entry.matches("[a-z0-9_.\\-:/*]+");
        return Identifier.tryParse(isTag(entry) ? entry.substring(1) : entry) != null;
    }

    /** Patterns without a namespace ("*_door") match any mod; "mymod:*_door" only that mod. */
    private static Pattern compilePattern(String entry) {
        String full = entry.contains(":") ? entry : "*:" + entry;
        StringBuilder regex = new StringBuilder();
        for (char ch : full.toCharArray()) {
            if (ch == '*') regex.append(".*");
            else regex.append(Pattern.quote(String.valueOf(ch)));
        }
        return Pattern.compile(regex.toString());
    }

    /** How specific an entry's match on this item is: 3 exact ID, 2 pattern, 1 tag, 0 no match. */
    private static int matchStrength(String entry, Identifier id, ItemStack stack) {
        if (isTag(entry)) {
            Identifier tagId = Identifier.tryParse(entry.substring(1));
            return tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId)) ? 1 : 0;
        }
        if (isPattern(entry)) return compilePattern(entry).matcher(id.toString()).matches() ? 2 : 0;
        return id.equals(Identifier.tryParse(entry)) ? 3 : 0;
    }

    // ---------------------------------------------------------------- lookups

    public enum Display { NONE, BLOCK, ITEM }

    private transient Map<Identifier, Display> displayCache;
    private transient Map<Identifier, ItemSettings> settingsCache;

    /** Which list wins for this item: the most specific matching entry decides. */
    public Display displayFor(ItemStack stack) {
        if (stack.isEmpty()) return Display.NONE;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (displayCache == null) displayCache = new HashMap<>();
        return displayCache.computeIfAbsent(id, k -> {
            int asItem = best(itemsAsItem, id, stack);
            int asBlock = best(itemsAsBlock, id, stack);
            if (asItem == 0 && asBlock == 0) return Display.NONE;
            return asItem >= asBlock ? Display.ITEM : Display.BLOCK;
        });
    }

    private static int best(List<String> entries, Identifier id, ItemStack stack) {
        int best = 0;
        for (String e : entries) best = Math.max(best, matchStrength(e, id, stack));
        return best;
    }

    /** True if this item may be hung on a chain (it matches either list). */
    public boolean isHangable(ItemStack stack) {
        return displayFor(stack) != Display.NONE;
    }

    public boolean isDisplayedAsItem(ItemStack stack) {
        return displayFor(stack) == Display.ITEM;
    }

    /** Per-item settings: the most specific matching entry wins (ID, then pattern, then tag). Never null. */
    public ItemSettings settingsFor(Identifier id, ItemStack stack) {
        if (settingsCache == null) settingsCache = new HashMap<>();
        return settingsCache.computeIfAbsent(id, k -> {
            ItemSettings found = null;
            int strength = 0;
            for (Map.Entry<String, ItemSettings> e : perItem.entrySet()) {
                int s = matchStrength(e.getKey(), id, stack);
                if (s > strength) {
                    strength = s;
                    found = e.getValue();
                }
            }
            return found != null ? found : new ItemSettings();
        });
    }

    /** Call after tags reload so groups are matched again. */
    public void clearCaches() {
        displayCache = null;
        settingsCache = null;
    }
}
