package me.fullpage.nmslib;

import me.fullpage.nmslib.chunk.ChunkSource;
import me.fullpage.nmslib.chunk.VirtualChunk;
import me.fullpage.nmslib.packet.DigListener;
import me.fullpage.nmslib.packet.UseItemOnListener;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public interface NMSHandler {

    /**
     * Prepares a reusable client-only item model, or null when unsupported.
     */
    default PacketDisplay preparePacketItem(Location location, ItemStack item) { return PacketDisplay.item(location, item); }

    /**
     * Prepares reusable client-only text with in-place updates, or null when unsupported.
     */
    default PacketDisplay preparePacketText(Location location, String text) { return PacketDisplay.text(location, text); }

    /**
     * Sends a prepared group to one viewer, bundling it when the protocol supports bundles.
     */
    default void sendDisplayUpdates(Player viewer, Collection<PacketDisplay.Update> updates) { PacketDisplay.send(viewer, updates); }

    /**
     * Changes a loaded block with client/lighting updates, without drops or events.
     * Callers must check protection and fire their own break event before calling.
     * Returns false when the chunk is unloaded or the block did not change.
     */
    default boolean setBlock(Block block, org.bukkit.block.data.BlockData data, boolean applyPhysics) {
        java.util.Objects.requireNonNull(data, "data");
        if (!canChangeBlock(block) || block.getBlockData().equals(data)) return false;
        block.setBlockData(data, applyPhysics);
        return true;
    }

    /** Removes a loaded block without drops, XP, effects or a BlockBreakEvent. */
    default boolean breakBlock(Block block, boolean applyPhysics) {
        if (!canChangeBlock(block) || block.getType() == org.bukkit.Material.AIR) return false;
        block.setType(org.bukkit.Material.AIR, applyPhysics);
        return true;
    }

    /** Shared validation used by native implementations; never loads a chunk. */
    static boolean canChangeBlock(Block block) {
        java.util.Objects.requireNonNull(block, "block");
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Block changes must run on the server thread");
        }
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }

    void sendActionBar(Player player, String message);

    void sendTitle(Player player, String title, String subtitle);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void clearTitle(Player player);

    void sendJsonMessage(Player player, String json);

    boolean isMainHand(PlayerInteractEvent event);

    ItemStack getItemInMainHand(Player player);

    ItemStack getItemInUse(Player player);

    Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin);

    Enchantment lookupEnchantment(String name, int internalId);

    Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin);

    HashMap<EnchantInfo, Enchantment> registerEnchantments(Collection<EnchantInfo> enchantInfos, Plugin plugin);

    @Deprecated
    boolean registerEnchantment(Enchantment enchantment);

    boolean isRegistered(Enchantment enchantment);

    boolean isRegistered(String name, int internalId);

    boolean isGrown(Block block, BlockState blockState);

    void setCropToAdult(Block block, BlockState blockState);

    void setCropToBaby(Block block, BlockState blockState);

    void moveTo(LivingEntity entity, Location moveTo, float speed);

    void stopNavigation(LivingEntity entity);

    void setBiteTime(PlayerFishEvent event, int ticks);
    void simulateRodInteraction(Player player);

    boolean isInWater(Entity entity);

    void setVelocity(Entity entity, Vector vector);

    Vector getVelocity(Entity entity);

    /**
     *
     * @apiNote inverted is for legacy versions only
     */
    void setStairDirection(BlockState blockState, BlockFace blockFace, boolean inverted);

    /**
     * Spawns a client-only hologram. Returns its entity ID, or -1 when unsupported.
     */
    default int spawnPacketHologram(Player player, Location location, String text) {
        return -1;
    }

    /**
     * Removes client-only entities previously spawned for this player.
     */
    default void removePacketEntities(Player player, int... entityIds) {
    }

    /** True when addDigListener and addUseItemOnListener are implemented natively. */
    default boolean supportsPacketListeners() {
        return false;
    }

    /**
     * Registers a dig listener; close the handle to remove just this listener. Listeners that drop packets skip
     * vanilla reach and loaded-chunk checks, so they must validate the position themselves.
     * Throws UnsupportedOperationException when unsupported.
     */
    default AutoCloseable addDigListener(Plugin owner, DigListener listener) {
        throw new UnsupportedOperationException("Packet listeners are not supported on this server version");
    }

    /** Registers a use-item-on listener; close the handle to remove just this listener. Throws UnsupportedOperationException when unsupported. */
    default AutoCloseable addUseItemOnListener(Plugin owner, UseItemOnListener listener) {
        throw new UnsupportedOperationException("Packet listeners are not supported on this server version");
    }

    /** Removes every packet listener registered by the plugin. */
    default void removePacketListeners(Plugin owner) {
    }

    /** True when prepareChunk, sendChunk and resendChunk are implemented natively. */
    default boolean supportsVirtualChunks() {
        return false;
    }

    /**
     * Builds a client-only chunk from the source: every section gets the source's biome, or the loaded server chunk's biomes
     * (plains when it is not loaded) when it is null, and light comes from the source, or from the server when no section supplies it.
     * Callable from any thread when the source returns a non-null biome and supplies light for at least one section; otherwise
     * server thread only, and off it throws IllegalStateException without reading server state: a null biome before any other
     * source call, missing light right after the light calls. Throws IllegalArgumentException for an unknown biome or state id,
     * and UnsupportedOperationException when unsupported.
     */
    default VirtualChunk prepareChunk(org.bukkit.World world, int chunkX, int chunkZ, ChunkSource source) {
        throw new UnsupportedOperationException("Virtual chunks are not supported on this server version");
    }

    /** Replaces the viewer's copy of the chunk with a prepared one, skipped when the viewer is in another world; server thread only. */
    default void sendChunk(Player viewer, VirtualChunk chunk) {
        throw new UnsupportedOperationException("Virtual chunks are not supported on this server version");
    }

    /**
     * Sends the loaded server chunk at chunkX, chunkZ in the viewer's world to the viewer exactly as the server would (anti-xray
     * included), replacing a virtual chunk; no-op when the chunk is not loaded or not sent to that viewer. Server thread only.
     * Throws UnsupportedOperationException when unsupported.
     */
    default void resendChunk(Player viewer, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException("Virtual chunks are not supported on this server version");
    }

    /**
     * Adds the players to the viewer's tab list as listed entries, also when the viewer cannot see them: hidePlayer removes
     * hidden players from the tab list, and the server sends them no later tab updates, nor their removal on quit.
     * Server thread only. Throws UnsupportedOperationException when unsupported.
     */
    default void sendTabListAdd(Player viewer, Collection<? extends Player> players) {
        throw new UnsupportedOperationException("Tab list packets are not supported on this server version");
    }

    /** Removes the players with these ids from the viewer's tab list. Server thread only. Throws UnsupportedOperationException when unsupported. */
    default void sendTabListRemove(Player viewer, Collection<UUID> players) {
        throw new UnsupportedOperationException("Tab list packets are not supported on this server version");
    }

    /**
     * Sends the players' current latency, game mode and tab list name to every viewer, also for players a viewer cannot see
     * (the server only refreshes those for visible players). One packet is built and shared. Server thread only.
     * Throws UnsupportedOperationException when unsupported.
     */
    default void sendTabListUpdate(Collection<? extends Player> viewers, Collection<? extends Player> players) {
        throw new UnsupportedOperationException("Tab list packets are not supported on this server version");
    }

    /**
     * Sets the item's name (item_name) and lore from legacy section-sign text, hex colours included, styled exactly as Adventure's
     * legacySection parse of the same text; a component is only replaced when it differs. No-op on empty items. Returns true when
     * the item changed. Throws UnsupportedOperationException when unsupported.
     */
    default boolean setItemDisplay(ItemStack item, String name, java.util.List<String> lore) {
        throw new UnsupportedOperationException("Item display components are not supported on this server version");
    }

    /**
     * The item's current name (item_name) as an opaque token to compare by identity: the same instance while nothing replaces
     * the name, copies included. Throws UnsupportedOperationException when unsupported.
     */
    default Object itemNameToken(ItemStack item) {
        throw new UnsupportedOperationException("Item display components are not supported on this server version");
    }

    /** Like itemNameToken, for the item's lore. Throws UnsupportedOperationException when unsupported. */
    default Object itemLoreToken(ItemStack item) {
        throw new UnsupportedOperationException("Item display components are not supported on this server version");
    }

    /** Returns an opaque state id, valid for this server run. */
    default int blockStateId(org.bukkit.block.data.BlockData data) {
        return BlockStateIds.id(java.util.Objects.requireNonNull(data, "data"));
    }

    /** Returns a new BlockData for an id from blockStateId. */
    default org.bukkit.block.data.BlockData blockData(int stateId) {
        return BlockStateIds.data(stateId);
    }

    /** Current world DataVersion of the server. Throws UnsupportedOperationException when unsupported. */
    default int dataVersion() {
        try {
            return org.bukkit.Bukkit.getUnsafe().getDataVersion();
        } catch (NoSuchMethodError e) {
            throw new UnsupportedOperationException("Data versions are not supported on this server version");
        }
    }

    /**
     * Upgrades block data such as {@code minecraft:chain[axis=y]} saved at fromDataVersion to the current version, properties sorted
     * by name; returns the input unchanged when it is current, malformed or unknown. Thread-safe. Throws UnsupportedOperationException when unsupported.
     */
    default String upgradeBlockData(String blockData, int fromDataVersion) {
        throw new UnsupportedOperationException("Data fixing is not supported on this server version");
    }

    /**
     * Upgrades a block entity SNBT compound with its id (x, y, z optional) saved at fromDataVersion to the current version;
     * returns the input unchanged when it is current or cannot be parsed or fixed. Thread-safe. Throws UnsupportedOperationException when unsupported.
     */
    default String upgradeBlockEntity(String snbt, int fromDataVersion) {
        throw new UnsupportedOperationException("Data fixing is not supported on this server version");
    }

    /** Sends client-only block changes; positions come from BlockPositions.pack, ids from blockStateId. */
    default void sendBlockChanges(Player viewer, long[] positions, int[] stateIds, int count) {
        checkBlockChanges(positions, stateIds, count);
        org.bukkit.World world = viewer.getWorld();
        for (int i = 0; i < count; i++) {
            long position = positions[i];
            viewer.sendBlockChange(new Location(world, BlockPositions.x(position), BlockPositions.y(position), BlockPositions.z(position)), blockData(stateIds[i]));
        }
    }

    /** Acknowledges client block predictions up to a dig packet sequence; server thread only, negatives ignored. */
    default void acknowledgeBlockChanges(Player player, int sequence) {
    }

    /**
     * Fills the inclusive box in loaded chunks only, without physics, drops or events, picking each state by cumulative weight.
     * Server thread only. Returns the number of changed blocks.
     */
    default int fillBlocks(org.bukkit.World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                           int[] stateIds, int[] cumulativeWeights, long seed) {
        checkFill(world, stateIds, cumulativeWeights);
        org.bukkit.block.data.BlockData[] data = new org.bukkit.block.data.BlockData[stateIds.length];
        for (int i = 0; i < data.length; i++) data[i] = blockData(stateIds[i]);
        java.util.SplittableRandom random = new java.util.SplittableRandom(seed);
        int total = cumulativeWeights[cumulativeWeights.length - 1];
        maxY = Math.min(maxY, world.getMaxHeight() - 1);
        int changed = 0;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                int toX = Math.min(maxX, (cx << 4) + 15), toZ = Math.min(maxZ, (cz << 4) + 15);
                for (int x = Math.max(minX, cx << 4); x <= toX; x++) {
                    for (int z = Math.max(minZ, cz << 4); z <= toZ; z++) {
                        for (int y = maxY; y >= minY; y--) {
                            Block block = world.getBlockAt(x, y, z);
                            org.bukkit.block.data.BlockData old = block.getBlockData();
                            // VOID_AIR is only returned below the world's min height (no getMinHeight in the 1.13 API)
                            if (old.getMaterial() == org.bukkit.Material.VOID_AIR) break;
                            org.bukkit.block.data.BlockData next = data[0];
                            if (data.length > 1) {
                                int roll = random.nextInt(total), i = 0;
                                while (roll >= cumulativeWeights[i]) i++;
                                next = data[i];
                            }
                            if (old.equals(next)) continue;
                            block.setBlockData(next, false);
                            changed++;
                        }
                    }
                }
            }
        }
        return changed;
    }

    /** Shared validation used by native implementations. */
    static void checkBlockChanges(long[] positions, int[] stateIds, int count) {
        if (count < 0 || count > positions.length || count > stateIds.length) {
            throw new IllegalArgumentException("count " + count + " is negative or exceeds positions/stateIds length");
        }
    }

    /** Shared validation used by native implementations. */
    static void checkFill(org.bukkit.World world, int[] stateIds, int[] cumulativeWeights) {
        java.util.Objects.requireNonNull(world, "world");
        if (stateIds.length == 0 || stateIds.length != cumulativeWeights.length) {
            throw new IllegalArgumentException("stateIds and cumulativeWeights must be non-empty and of equal length");
        }
        for (int i = 0, previous = 0; i < cumulativeWeights.length; previous = cumulativeWeights[i++]) {
            if (cumulativeWeights[i] <= previous) {
                throw new IllegalArgumentException("cumulativeWeights must be positive and strictly increasing");
            }
        }
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Block changes must run on the server thread");
        }
    }

}
