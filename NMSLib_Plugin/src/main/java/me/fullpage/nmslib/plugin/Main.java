package me.fullpage.nmslib.plugin;

import me.fullpage.nmslib.NMSHandler;
import net.minecraft.server.v1_8_R3.BlockPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.material.Banner;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements NMSHandler {

    private NMSHandler nmsHandler;

    @Override
    public void onEnable() {
        this.nmsHandler = NMSLib.init(this);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        this.nmsHandler.sendActionBar(player, message);
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        this.nmsHandler.sendJsonMessage(player, json);
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
