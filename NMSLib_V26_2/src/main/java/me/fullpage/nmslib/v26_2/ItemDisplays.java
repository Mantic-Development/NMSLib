package me.fullpage.nmslib.v26_2;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.component.ItemLore;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Item names and lore from legacy text as eager vanilla components, so the server compares and encodes them without Adventure conversion. */
final class ItemDisplays {
    private ItemDisplays() { }

    private static final int SHIFT = 18;
    // Direct-mapped and racy by design: a slot holds one immutable Line, so a lost write only costs another parse.
    static final Line[] LINES = new Line[1 << (32 - SHIFT)];
    private static final String CODES = "0123456789abcdefklmnor";

    /** A parsed line and its lore-styled form, shared by every item showing it and never mutated. */
    record Line(String legacy, Component component, Component styled) { }

    static boolean set(ItemStack item, String name, List<String> lore) {
        net.minecraft.world.item.ItemStack handle = CraftItemStack.unwrap(item);
        if (handle.isEmpty()) return false;
        boolean changed = false;
        Component title = line(name).component(), currentName = handle.get(DataComponents.ITEM_NAME);
        // Paper's AdventureComponent equals its conversion; replace it anyway so the codecs stop going through Adventure.
        if (!(currentName instanceof MutableComponent) || !title.equals(currentName)) {
            handle.set(DataComponents.ITEM_NAME, title);
            changed = true;
        }
        List<Component> current = handle.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines();
        int size = lore.size();
        Component[] lines = new Component[size], styled = new Component[size];
        boolean same = current.size() == size;
        for (int i = 0; i < size; i++) {
            Line line = line(lore.get(i));
            lines[i] = line.component();
            styled[i] = line.styled();
            same = same && current.get(i) instanceof MutableComponent at && lines[i].equals(at);
        }
        if (!same) {
            // ItemLore.equals compares styled lines first; vanilla's lazy view would merge styles again on every comparison.
            handle.set(DataComponents.LORE, new ItemLore(Arrays.asList(lines), Arrays.asList(styled)));
            changed = true;
        }
        return changed;
    }

    static Line line(String legacy) {
        int slot = legacy.hashCode() * 0x9E3779B9 >>> SHIFT;
        Line line = LINES[slot];
        if (line == null || !line.legacy().equals(legacy)) {
            Component component = parse(legacy);
            LINES[slot] = line = new Line(legacy, component, new ItemLore(List.of(component)).styledLines().getFirst());
        }
        return line;
    }

    /**
     * Adventure's legacySection deserializer, flattened. Codes are found right to left as it does, so odd sequences split the
     * same way; a colour or reset clears decorations and leaves unset ones to the parent, like Adventure's NOT_SET.
     */
    static Component parse(String legacy) {
        int next = legacy.lastIndexOf('§', legacy.length() - 2);
        if (next < 0) return Component.literal(legacy);
        List<Object> formats = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        int pos = legacy.length();
        do {
            int at = next + 2, from = at, start = next;
            Object format;
            if (at >= 14 && legacy.charAt(at - 14) == '§' && legacy.charAt(at - 13) == 'x') {
                char[] hex = new char[6];
                for (int i = 0; i < 6; i++) hex[i] = legacy.charAt(at - 11 + 2 * i);
                format = rgb(new String(hex));
                start = next - 12;
            } else if (legacy.charAt(next + 1) == '#' && legacy.length() - at >= 6) {
                format = rgb(legacy.substring(at, at + 6));
                from = next + 8;
            } else {
                format = code(legacy.charAt(next + 1));
            }
            if (format != null) {
                formats.add(format);
                texts.add(legacy.substring(from, pos));
                next = pos = start;
            }
            next = legacy.lastIndexOf('§', next - 1);
        } while (next >= 0);
        MutableComponent root = Component.literal(legacy.substring(0, pos));
        Style style = Style.EMPTY;
        for (int i = formats.size() - 1; i >= 0; i--) {
            Object format = formats.get(i);
            style = format instanceof TextColor color ? Style.EMPTY.withColor(color)
                    : format == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat((ChatFormatting) format);
            String text = texts.get(i);
            if (!text.isEmpty()) root.append(Component.literal(text).setStyle(style));
        }
        return pos == 0 && root.getSiblings().size() == 1 ? root.getSiblings().getFirst() : root;
    }

    private static TextColor rgb(String hex) {
        try {
            return TextColor.fromRgb(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A colour as TextColor, a decoration or reset as ChatFormatting; ASCII codes only, like Adventure. */
    private static Object code(char c) {
        char lower = c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
        if (CODES.indexOf(lower) < 0) return null;
        ChatFormatting format = ChatFormatting.getByCode(lower);
        TextColor color = TextColor.fromLegacyFormat(format);
        return color != null ? color : format;
    }
}
