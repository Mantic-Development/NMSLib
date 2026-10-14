package me.fullpage.nmslib.v26_2;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Paper's DataConverter upgrades; the server runs it concurrently on chunk loader threads, so these run on any thread. */
final class DataUpgrades {

    private DataUpgrades() {
    }

    static int current() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    static String blockData(String blockData, int fromDataVersion) {
        Objects.requireNonNull(blockData, "blockData");
        int to = current();
        if (fromDataVersion >= to) return blockData;
        int open = blockData.indexOf('['), close = blockData.lastIndexOf(']');
        if (open >= 0 && close < open) return blockData;
        String name = open < 0 ? blockData : blockData.substring(0, open);
        CompoundTag state = new CompoundTag();
        state.putString("Name", name.indexOf(':') < 0 ? "minecraft:" + name : name);
        if (open >= 0) {
            CompoundTag properties = new CompoundTag();
            for (String pair : blockData.substring(open + 1, close).split(",")) {
                int equals = pair.indexOf('=');
                if (equals > 0) properties.putString(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
            }
            state.put("Properties", properties);
        }
        try {
            CompoundTag fixed = MCDataConverter.convertTag(MCTypeRegistry.BLOCK_STATE, state, fromDataVersion, to);
            CompoundTag fixedProperties = fixed.getCompoundOrEmpty("Properties");
            // Sorted by name like vanilla BlockState strings.
            Map<String, String> properties = new TreeMap<>();
            for (String key : fixedProperties.keySet()) properties.put(key, fixedProperties.getStringOr(key, ""));
            StringBuilder out = new StringBuilder(fixed.getStringOr("Name", name));
            if (properties.isEmpty()) return out.toString();
            char separator = '[';
            for (Map.Entry<String, String> property : properties.entrySet()) {
                out.append(separator).append(property.getKey()).append('=').append(property.getValue());
                separator = ',';
            }
            return out.append(']').toString();
        } catch (RuntimeException e) {
            return blockData;
        }
    }

    static String blockEntity(String snbt, int fromDataVersion) {
        Objects.requireNonNull(snbt, "snbt");
        int to = current();
        if (fromDataVersion >= to) return snbt;
        try {
            CompoundTag tag = TagParser.parseCompoundFully(snbt);
            // Structure converters would still touch a compound without an id, which the contract returns unchanged.
            if (tag.getStringOr("id", null) == null) return snbt;
            return MCDataConverter.convertTag(MCTypeRegistry.TILE_ENTITY, tag, fromDataVersion, to).toString();
        } catch (CommandSyntaxException | RuntimeException e) {
            return snbt;
        }
    }
}
