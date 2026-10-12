package me.fullpage.nmslib;

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

}
