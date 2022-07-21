package me.fullpage.nmslib_v1_8_r3;

import me.fullpage.nmslib.NMSHandler;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

public final class NMSLib_V1_8_R3 implements NMSHandler {

    @Override
    public void setBlock(Location location, Material material) {
        this.setBlock(location.getBlock(), material);

    }

    @Override
    public void setBlock(Location location, Material material, boolean applyPhysics) {
        this.setBlock(location.getBlock(), material, applyPhysics);
    }

    @Override
    public void setBlock(Location location, Material material, int durability) {
        this.setBlock(location.getBlock(), material, durability);

    }

    @Override
    public void setBlock(Location location, Material material, int durability, boolean applyPhysics) {
        this.setBlock(location.getBlock(), material, durability, applyPhysics);

    }

    @Override
    public void setBlock(Block block, Material material) {
        IBlockData iblockdata = CraftMagicNumbers.getBlock(block).getBlockData();
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.World nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(true));

    }

    @Override
    public void setBlock(Block block, Material material, boolean applyPhysics) {
        IBlockData iblockdata = CraftMagicNumbers.getBlock(block).getBlockData();
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.World nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(applyPhysics));

    }

    @Override
    public void setBlock(Block block, Material material, int durability) {
        IBlockData iblockdata = CraftMagicNumbers.getBlock(block).fromLegacyData(durability);
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.World nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(true));

    }

    @Override
    public void setBlock(Block block, Material material, int durability, boolean applyPhysics) {
        IBlockData iblockdata = CraftMagicNumbers.getBlock(block).fromLegacyData(durability);
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.World nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(applyPhysics));
    }

    @Override
    public void setBlock(World world, int x, int y, int z, Material material, boolean applyPhysics) {
        BlockPosition blockPosition = this.getBlockPosition(x, y, z);
        net.minecraft.server.v1_8_R3.World nmsWorld = this.getWorld(world);
        nmsWorld.setTypeAndData(blockPosition, this.iBlockDataFromMaterial(material), this.getApplyPhysicsId(applyPhysics));

    }

    @Override
    public void sendActionBar(Player player, String message) {
        PacketPlayOutChat packet = new PacketPlayOutChat(new ChatComponentText(message), (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(json)));
    }

    private BlockPosition getBlockPosition(int x, int y, int z) {
        return new BlockPosition(x, y, z);
    }

    private int getApplyPhysicsId(boolean applyPhysics) {
        return applyPhysics ? 3 : 2;
    }

    private IBlockData iBlockDataFromMaterial(Material m) {
        net.minecraft.server.v1_8_R3.Block nmsBlock = CraftMagicNumbers.getBlock(m);
        return nmsBlock.getBlockData();
    }

    private net.minecraft.server.v1_8_R3.World getWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

}
