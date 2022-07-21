package me.fullpage.nmslib;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface NMSHandler {

    // TODO: Implementive native chunk placement

    void setBlock(Location location, Material material);

    void setBlock(Location location, Material material, boolean applyPhysics);

    void setBlock(Location location, Material material, int durability);

    void setBlock(Location location, Material material, int durability, boolean applyPhysics);

    void setBlock(Block block, Material material);

    void setBlock(Block block, Material material, boolean applyPhysics);

    void setBlock(Block block, Material material, int durability);

    void setBlock(Block block, Material material, int durability, boolean applyPhysics);


    void setBlock(World world, int x, int y, int z, Material material, boolean applyPhysics);

    // void setBlockInNativeChunk(World world, int x, int y, int z, int blockId, byte data, boolean applyPhysics);

    void sendActionBar(Player player, String message);

    void sendJsonMessage(Player player, String json);

}
