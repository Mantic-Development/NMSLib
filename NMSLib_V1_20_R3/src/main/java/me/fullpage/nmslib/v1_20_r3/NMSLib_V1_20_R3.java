package me.fullpage.nmslib.v1_20_r3;

import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.MinecraftKey;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R3.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;

public final class NMSLib_V1_20_R3 implements NMSHandler {


    public NMSLib_V1_20_R3() {
        ((CraftMagicNumbers) CraftMagicNumbers.INSTANCE).getMappingsVersion();
    }

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        player.spigot().sendMessage(ComponentSerializer.parse(json));
    }

    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        return event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;
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
    public ItemStack getItemInMainHand(Player player) {
        return player == null ? null : player.getInventory().getItemInMainHand();
    }


    @Override
    public Enchantment lookupEnchantment(String name, int internalId) {
        for (Enchantment value : Enchantment.values()) {
            if (value == null) continue;
            NamespacedKey key = value.getKey();
            if (key.getKey().equals(name) || name.equals(key.getNamespace() + ":" + key.getKey())) {
                return value;
            }
        }
        return null;
    }


    @Override
    public org.bukkit.enchantments.Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, enchantInfo.getName());
        return new ManticApiEnchant(key, enchantInfo);
    }


    @Override
    public Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin) {

        unfreezeRegistry();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, enchantInfo.getName());
        ManticServerEnchant entry = new ManticServerEnchant(enchantInfo);
        IRegistry.a(BuiltInRegistries.f, namespacedKey.getKey(), entry);
        freezeRegistry();
        return CraftEnchantment.minecraftToBukkit(entry);
    }

    @Override
    public boolean registerEnchantment(org.bukkit.enchantments.Enchantment enchantment) {
        throw new UnsupportedOperationException("This method is not supported in 1.20.4 and above. Use registerEnchantment(EnchantInfo, Plugin) instead.");

    }

    public void unfreezeRegistry() {
        try {
            IRegistry<net.minecraft.world.item.enchantment.Enchantment> f = BuiltInRegistries.f;
            Class<? extends IRegistry> aClass = f.getClass();
            // set "l" field to false
            Field l = aClass.getDeclaredField("l");
            l.setAccessible(true);
            l.set(f, false);
            l.setAccessible(false);


            Field m = aClass.getDeclaredField("m");
            m.setAccessible(true);
            m.set(f, new IdentityHashMap<>());
            m.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void freezeRegistry() {
        BuiltInRegistries.f.l();
    }

    private void registerEnchantment(Plugin plugin, String name, EnchantInfo enchantInfo) {
        System.out.println("Debug 3");
        Enchantment enchantment = lookupEnchantment(name, enchantInfo.getInternalId());
        if (enchantment != null) {
            return;
        }
        MinecraftKey minecraftKey = new MinecraftKey(new NamespacedKey(plugin, enchantInfo.getName()).getNamespace(), name);

        unfreezeRegistry();
        IRegistry.a(BuiltInRegistries.f, minecraftKey, new ManticServerEnchant(enchantInfo));
        freezeRegistry();
    }

    @Override
    public boolean isRegistered(org.bukkit.enchantments.Enchantment enchantment) {
        for (org.bukkit.enchantments.Enchantment value : Enchantment.values()) {
            if (value.equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

}
