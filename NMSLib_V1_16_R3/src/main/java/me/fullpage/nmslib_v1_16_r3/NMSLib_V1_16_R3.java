package me.fullpage.nmslib_v1_16_r3;

import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class NMSLib_V1_16_R3 implements NMSHandler {
    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }


    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 20, 10);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void clearTitle(Player player) {
        player.resetTitle();
    }
    @Override
    public void sendJsonMessage(Player player, String json) {
        IChatMutableComponent a = IChatBaseComponent.ChatSerializer.a(json);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(a, net.minecraft.server.v1_16_R3.ChatMessageType.CHAT, player.getUniqueId()));
    }


    @Override
    public ItemStack getItemInMainHand(Player player) {
        return player == null ? null : player.getInventory().getItemInMainHand();
    }

}
