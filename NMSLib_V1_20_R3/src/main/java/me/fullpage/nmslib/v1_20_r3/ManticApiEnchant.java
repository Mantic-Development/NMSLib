package me.fullpage.nmslib.v1_20_r3;

import io.papermc.paper.enchantments.EnchantmentRarity;
import me.fullpage.nmslib.EnchantInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public class ManticApiEnchant extends Enchantment {

    private final NamespacedKey key;
    private final EnchantInfo enchantInfo;


    public ManticApiEnchant(NamespacedKey key, EnchantInfo enchantInfo) {
        this.key = key;
        this.enchantInfo = enchantInfo;
    }

    @Override
    public @NotNull String translationKey() {
        return "";
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
    public EnchantmentTarget getItemTarget() {
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
    public boolean conflictsWith(org.bukkit.enchantments.Enchantment enchantment) {
        return enchantInfo.conflictsWith(enchantment);
    }

    @Override
    public boolean canEnchantItem(ItemStack itemStack) {
        return enchantInfo.canEnchantItem(itemStack);
    }

    @Override
    public @NotNull Component displayName(int i) {
        return Component.empty();
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public int getMinModifiedCost(int i) {
        return 0;
    }

    @Override
    public int getMaxModifiedCost(int i) {
        return 0;
    }

    @Override
    public @NotNull EnchantmentRarity getRarity() {
        return EnchantmentRarity.COMMON;
    }

    @Override
    public float getDamageIncrease(int i, @NotNull EntityCategory entityCategory) {
        return 0;
    }

    @Override
    public @NotNull Set<EquipmentSlot> getActiveSlots() {
        EquipmentSlot[] values = EquipmentSlot.values();
        Set<EquipmentSlot> slots = new java.util.HashSet<>(values.length);
        slots.addAll(Arrays.asList(values));
        return slots;
    }

}
