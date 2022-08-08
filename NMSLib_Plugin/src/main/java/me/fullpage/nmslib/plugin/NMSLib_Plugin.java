package me.fullpage.nmslib.plugin;

import me.fullpage.nmslib.NMSHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class NMSLib_Plugin extends JavaPlugin implements NMSHandler {

    private NMSHandler nmsHandler;

    @Override
    public void onEnable() {
        this.nmsHandler = NMSLib.init(this);
    }

    @Override
    public void setBlock(Location location, Material material) {
        this.nmsHandler.setBlock(location, material);
    }

    @Override
    public void setBlock(Location location, Material material, boolean applyPhysics) {
        this.nmsHandler.setBlock(location, material, applyPhysics);
    }

    @Override
    public void setBlock(Location location, Material material, int durability) {
        this.nmsHandler.setBlock(location, material, durability);
    }

    @Override
    public void setBlock(Location location, Material material, int durability, boolean applyPhysics) {
        this.nmsHandler.setBlock(location, material, durability, applyPhysics);
    }

    @Override
    public void setBlock(Block block, Material material) {
        this.nmsHandler.setBlock(block, material);
    }

    @Override
    public void setBlock(Block block, Material material, boolean applyPhysics) {
        this.nmsHandler.setBlock(block, material, applyPhysics);
    }

    @Override
    public void setBlock(Block block, Material material, int durability) {
        this.nmsHandler.setBlock(block, material, durability);
    }

    @Override
    public void setBlock(Block block, Material material, int durability, boolean applyPhysics) {
        this.nmsHandler.setBlock(block, material, durability, applyPhysics);
    }

    @Override
    public void setBlock(World world, int x, int y, int z, Material material, boolean applyPhysics) {
        this.nmsHandler.setBlock(world, x, y, z, material, applyPhysics);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        this.nmsHandler.sendActionBar(player, message);
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        this.nmsHandler.sendJsonMessage(player, json);
    }

}
