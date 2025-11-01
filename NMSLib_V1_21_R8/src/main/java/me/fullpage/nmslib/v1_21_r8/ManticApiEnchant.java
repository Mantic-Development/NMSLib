package me.fullpage.nmslib.v1_21_r8;

import io.papermc.paper.enchantments.EnchantmentRarity;
import io.papermc.paper.registry.set.RegistryKeySet;
import me.fullpage.nmslib.EnchantInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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

    @Override
    public @NotNull Component displayName(int i) {
        return null;
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
    public int getAnvilCost() {
        return 0;
    }

    @Override
    public @NotNull EnchantmentRarity getRarity() {
        return null;
    }

    @Override
    public float getDamageIncrease(int i, @NotNull EntityCategory entityCategory) {
        return 0;
    }

    @Override
    public float getDamageIncrease(int i, @NotNull EntityType entityType) {
        return 0;
    }

    @Override
    public @NotNull Set<EquipmentSlotGroup> getActiveSlotGroups() {
        return Set.of();
    }

    @Override
    public @NotNull Component description() {
        return null;
    }

    @Override
    public @NotNull RegistryKeySet<ItemType> getSupportedItems() {
        return null;
    }

    @Override
    public @Nullable RegistryKeySet<ItemType> getPrimaryItems() {
        return null;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public @NotNull RegistryKeySet<Enchantment> getExclusiveWith() {
        return null;
    }

    @Override
    public @NotNull String translationKey() {
        return "";
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return "";
    }

}
