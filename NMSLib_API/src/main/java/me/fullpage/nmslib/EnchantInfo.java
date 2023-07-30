package me.fullpage.nmslib;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

public abstract class EnchantInfo {

    private final String name;
    private final int internalId;

    public EnchantInfo(String name, int internalId) {
        this.name = name;
        this.internalId = internalId;
    }

    public String getName() {
        return name;
    }

    public int getInternalId() {
        return internalId;
    }

    public abstract int getMaxLevel();

    public abstract int getStartLevel();

    public abstract EnchantmentTarget getItemTarget();

    public abstract boolean conflictsWith(Enchantment other);

    public abstract boolean canEnchantItem(ItemStack item);

    public abstract boolean isTreasure();

    public abstract boolean isCursed();

}
