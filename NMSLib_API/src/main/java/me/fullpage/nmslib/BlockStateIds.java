package me.fullpage.nmslib;

import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bukkit fallback for state ids: interns BlockData strings to sequential ids. */
final class BlockStateIds {

    private static final Map<String, Integer> IDS = new HashMap<>();
    private static final List<BlockData> DATA = new ArrayList<>();

    private BlockStateIds() {
    }

    static synchronized int id(BlockData data) {
        String key = data.getAsString();
        Integer id = IDS.get(key);
        if (id != null) return id;
        DATA.add(data.clone());
        IDS.put(key, DATA.size() - 1);
        return DATA.size() - 1;
    }

    static synchronized BlockData data(int id) {
        if (id < 0 || id >= DATA.size()) throw new IllegalArgumentException("Unknown block state id " + id);
        return DATA.get(id).clone();
    }
}
