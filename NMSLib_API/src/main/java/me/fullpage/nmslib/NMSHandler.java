package me.fullpage.nmslib;

import me.fullpage.nmslib.packet.DigListener;
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

    /** True when addDigListener is implemented natively. */
    default boolean supportsPacketListeners() {
        return false;
    }

    /** Registers a dig listener owned by the plugin; throws UnsupportedOperationException when unsupported. */
    default void addDigListener(Plugin owner, DigListener listener) {
        throw new UnsupportedOperationException("Packet listeners are not supported on this server version");
    }

    /** Removes every packet listener registered by the plugin. */
    default void removePacketListeners(Plugin owner) {
    }

    /** Returns an opaque state id, valid for this server run. */
    default int blockStateId(org.bukkit.block.data.BlockData data) {
        return BlockStateIds.id(java.util.Objects.requireNonNull(data, "data"));
    }

    /** Returns a new BlockData for an id from blockStateId. */
    default org.bukkit.block.data.BlockData blockData(int stateId) {
        return BlockStateIds.data(stateId);
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
