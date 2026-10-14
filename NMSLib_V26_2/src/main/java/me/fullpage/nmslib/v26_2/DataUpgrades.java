package me.fullpage.nmslib.v26_2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** DataFixer upgrades; DFU and the SNBT parser are thread-safe, so these run on any thread. */
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
            Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.BLOCK_STATE, new Dynamic<>(NbtOps.INSTANCE, state), fromDataVersion, to);
            // Sorted by name like vanilla BlockState strings.
            Map<String, String> properties = new TreeMap<>(fixed.get("Properties").asMap(key -> key.asString(""), value -> value.asString("")));
            StringBuilder out = new StringBuilder(fixed.get("Name").asString(name));
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
            Dynamic<Tag> tag = new Dynamic<>(NbtOps.INSTANCE, TagParser.parseCompoundFully(snbt));
            return DataFixers.getDataFixer().update(References.BLOCK_ENTITY, tag, fromDataVersion, to).getValue().toString();
        } catch (CommandSyntaxException | RuntimeException e) {
            return snbt;
        }
    }
}
