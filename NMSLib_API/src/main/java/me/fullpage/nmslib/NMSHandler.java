package me.fullpage.nmslib;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public interface NMSHandler {

    void sendActionBar(Player player, String message);

    void sendTitle(Player player, String title, String subtitle);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void clearTitle(Player player);

    void sendJsonMessage(Player player, String json);

    boolean isMainHand(PlayerInteractEvent event);

    ItemStack getItemInMainHand(Player player);

    Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin);
    Enchantment lookupEnchantment(String name, int internalId);

    Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin);

    @Deprecated
    boolean registerEnchantment(Enchantment enchantment);

    boolean isRegistered(Enchantment enchantment);

    boolean isRegistered(String name, int internalId);

}
