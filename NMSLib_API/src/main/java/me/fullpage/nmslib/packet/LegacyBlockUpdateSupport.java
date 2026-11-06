package me.fullpage.nmslib.packet;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

/**
 * Block update filtering on servers without the Bukkit BlockData API.
 */
public interface LegacyBlockUpdateSupport {
    AutoCloseable addBlockUpdateFilter(Plugin owner, BlockUpdateFilter filter);

    int legacyBlockStateId(Material material, byte data);
}
