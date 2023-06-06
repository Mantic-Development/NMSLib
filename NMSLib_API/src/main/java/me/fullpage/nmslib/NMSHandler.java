package me.fullpage.nmslib;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NMSHandler {

    void sendActionBar(Player player, String message);

    void sendTitle(Player player, String title, String subtitle);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void clearTitle(Player player);

    void sendJsonMessage(Player player, String json);

    ItemStack getItemInMainHand(Player player);

}
