package me.fullpage.nmslib.plugin;

import me.fullpage.manticlib.command.ManticCommand;
import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements NMSHandler {

    private NMSHandler nmsHandler;

    @Override
    public void onEnable() {
        this.nmsHandler = NMSLib.init(this);
        ManticCommand.register(new TestCmd());
    }

    @Override
    public void sendActionBar(Player player, String message) {
        this.nmsHandler.sendActionBar(player, message);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        this.nmsHandler.sendTitle(player, title, subtitle);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.nmsHandler.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void clearTitle(Player player) {
        this.nmsHandler.clearTitle(player);
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        this.nmsHandler.sendJsonMessage(player, json);
    }

    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        return this.nmsHandler.isMainHand(event);
    }

    @Override
    public ItemStack getItemInMainHand(Player player) {
        return player == null ? null : this.nmsHandler.getItemInMainHand(player);
    }

    @Override
    public Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        return this.nmsHandler.buildEnchantment(enchantInfo, plugin);
    }

    @Override
    public Enchantment lookupEnchantment(String name, int internalId) {
        return this.nmsHandler.lookupEnchantment(name, internalId);
    }



    @Override
    public Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        try {
            Enchantment enchantment = buildEnchantment(enchantInfo, plugin);
            registerEnchantment(enchantment);
            return enchantment;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean registerEnchantment(Enchantment enchantment) {
        return this.nmsHandler.registerEnchantment(enchantment);
    }

    @Override
    public boolean isRegistered(Enchantment enchantment) {
        return this.nmsHandler.isRegistered(enchantment);
    }

    @Override
    public boolean isRegistered(String name, int internalId) {
        return this.nmsHandler.isRegistered(name, internalId);
    }


    public void setBlock(Location location, Material material) {
        this.setBlock(location.getBlock(), material);

    }

    public void setBlock(Location location, Material material, boolean applyPhysics) {
        this.setBlock(location.getBlock(), material, applyPhysics);
    }

    public void setBlock(Location location, Material material, int durability) {
        this.setBlock(location.getBlock(), material, durability);

    }

    public void setBlock(Location location, Material material, int durability, boolean applyPhysics) {
        this.setBlock(location.getBlock(), material, durability, applyPhysics);

    }

    public void setBlock(Block block, Material material) {
        this.setBlock(block, material, true);
    }

    public void setBlock(Block block, Material material, boolean applyPhysics) {
        block.setType(material, applyPhysics);
    }

    public void setBlock(Block block, Material material, int durability) {
        this.setBlock(block, material, durability, true);
    }

    public void setBlock(Block block, Material material, int durability, boolean applyPhysics) {
        BlockState state = block.getState();
        state.setData(new MaterialData(material, (byte) durability));
        state.update(true, applyPhysics);
    }

    public void setBlock(World world, int x, int y, int z, Material material, boolean applyPhysics) {
        world.getBlockAt(x, y, z).setType(material, applyPhysics);
    }


}
