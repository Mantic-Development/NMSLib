package me.fullpage.nmslib.v1_21_r7;

import me.fullpage.manticlib.nmslib.plugin.NMSLib;
import me.fullpage.nmslib.EnchantInfo;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ManticApiEnchant extends Enchantment {

    private final NamespacedKey key;
    private final EnchantInfo enchantInfo;

    public ManticApiEnchant(NamespacedKey key, EnchantInfo enchantInfo) {
        this.key = key;
        this.enchantInfo = enchantInfo;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return key;
    }

    @Override
    public String getName() {
        return enchantInfo.getName();
    }

    @Override
    public int getMaxLevel() {
        return enchantInfo.getMaxLevel();
    }

    @Override
    public int getStartLevel() {
        return enchantInfo.getStartLevel();
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return enchantInfo.getItemTarget();
    }

    @Override
    public boolean isTreasure() {
        return enchantInfo.isTreasure();
    }

    @Override
    public boolean isCursed() {
        return enchantInfo.isCursed();
    }

    @Override
    public boolean conflictsWith(Enchantment enchantment) {
        return enchantInfo.conflictsWith(enchantment);
    }

    @Override
    public boolean canEnchantItem(ItemStack itemStack) {
        return enchantInfo.canEnchantItem(itemStack);
    }


    @NotNull
    @Override
    public String getTranslationKey() {
        return "";
    }

    @Override
    public @NonNull NamespacedKey getKeyOrThrow() {
        return NamespacedKey.minecraft(enchantInfo.getName());
    }

    @Override
    public @Nullable NamespacedKey getKeyOrNull() {
        return NamespacedKey.minecraft(enchantInfo.getName());
    }

    @Override
    public boolean isRegistered() {
        return NMSLib.getNmsHandler().isRegistered(this);
    }
}
