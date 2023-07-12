package me.fullpage.nmslib.v1_12_r1;

import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class NMSLib_V1_12_R1 implements NMSHandler {

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(json)));
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 20, 10);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        PacketPlayOutTitle packetSubtitle;
        if (title != null) {
            packetSubtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, CraftChatMessage.fromString(title)[0], fadeIn, stay, fadeIn);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetSubtitle);
        }

        if (subtitle != null) {
            packetSubtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, CraftChatMessage.fromString(subtitle)[0], fadeIn, stay, fadeIn);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetSubtitle);
        }
    }

    @Override
    public void clearTitle(Player player) {
        PacketPlayOutTitle packetReset = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.RESET, null);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetReset);
    }


    @Override
    public ItemStack getItemInMainHand(Player player) {
        PlayerInventory inventory;
        return player == null ? null : ((inventory = player.getInventory()) == null ? null : inventory.getItemInMainHand());
    }

}
