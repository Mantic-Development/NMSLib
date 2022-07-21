package me.fullpage.nmslib_v1_17_r1;

import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

public final class NMSLib_V1_17_R1 implements NMSHandler {

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
        IBlockData iblockdata = this.iBlockDataFromMaterial(material);
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        WorldServer nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(true));

    }

    @Override
    public void setBlock(Block block, Material material, boolean applyPhysics) {
        IBlockData iblockdata = this.iBlockDataFromMaterial(material);
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        WorldServer nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(applyPhysics));

    }

    @Override
    public void setBlock(Block block, Material material, int durability) {
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        WorldServer nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, CraftMagicNumbers.getBlock(material, (byte) durability), this.getApplyPhysicsId(true));

    }

    @Override
    public void setBlock(Block block, Material material, int durability, boolean applyPhysics) {
        IBlockData iblockdata = CraftMagicNumbers.getBlock(material, (byte) durability);
        BlockPosition blockPosition = this.getBlockPosition(block.getX(), block.getY(), block.getZ());
        WorldServer nmsWorld = this.getWorld(block.getWorld());
        nmsWorld.setTypeAndData(blockPosition, iblockdata, this.getApplyPhysicsId(applyPhysics));
    }

    @Override
    public void setBlock(World world, int x, int y, int z, Material material, boolean applyPhysics) {
        BlockPosition blockPosition = this.getBlockPosition(x, y, z);
        WorldServer nmsWorld = this.getWorld(world);
        nmsWorld.setTypeAndData(blockPosition, this.iBlockDataFromMaterial(material), this.getApplyPhysicsId(applyPhysics));

    }

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        final IChatMutableComponent a = IChatBaseComponent.ChatSerializer.a(json);
        ((CraftPlayer) player).getHandle().b.sendPacket(new PacketPlayOutChat(a, net.minecraft.network.chat.ChatMessageType.a, player.getUniqueId()));
    }

    private BlockPosition getBlockPosition(int x, int y, int z) {
        return new BlockPosition(x, y, z);
    }

    private int getApplyPhysicsId(boolean applyPhysics) {
        return applyPhysics ? 3 : 2;
    }

    private IBlockData iBlockDataFromMaterial(Material m) {
        net.minecraft.world.level.block.Block nmsBlock = CraftMagicNumbers.getBlock(m);
        return nmsBlock.getBlockData();
    }

    private WorldServer getWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

}
