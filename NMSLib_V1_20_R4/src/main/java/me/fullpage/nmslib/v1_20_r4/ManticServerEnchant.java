package me.fullpage.nmslib.v1_20_r4;

import me.fullpage.nmslib.EnchantInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.craftbukkit.v1_20_R4.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_20_R4.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftItemStack;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public class ManticServerEnchant extends Enchantment {

    private final EnchantInfo enchant;

    public ManticServerEnchant(EnchantInfo enchant) {
        super(getDefinition(enchant));
        this.enchant = enchant;
    }

    @Override
    public Component getFullname(int level) {
        String name = enchant.getName();
        return Component.literal(name);
    }


    @Override
    public boolean isTreasureOnly() {
        return enchant.isTreasure() || enchant.isCursed();
    }

    @Override
    public boolean isDiscoverable() { // removes from enchantment table or use by items in loot tables
        return true;
    }

    @Override
    public boolean isCurse() {
        return enchant.isCursed();
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return enchant.conflictsWith(CraftEnchantment.minecraftToBukkit(other));
    }

    @Override
    public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
        return enchant.canEnchantItem(CraftItemStack.asBukkitCopy(stack));
    }

    private static EnchantmentDefinition getDefinition(EnchantInfo enchant) {
        TagKey<Item> category = nmsCategory(enchant);
        int weight = 10;
        /*
        * COMMON(10),
          UNCOMMON(5),
          RARE(2),
          VERY_RARE(1);
        * */

        int maxLevel = enchant.getMaxLevel();
        Cost minCost = new Cost(1, 0);
        Cost maxCost = new Cost(1, 0);
        int anvilCost = 10;
        net.minecraft.world.entity.EquipmentSlot[] slots = nmsSlots(enchant);

        return Enchantment.definition(category, weight, maxLevel, minCost, maxCost, anvilCost, slots);
    }

    public static TagKey<Item> nmsCategory(EnchantInfo data) {
        EnchantmentTarget itemTarget = data.getItemTarget();
        if (itemTarget == EnchantmentTarget.ALL) {
           itemTarget = EnchantmentTarget.BREAKABLE;
        }
        return switch (itemTarget) {
            case WEAPON -> ItemTags.WEAPON_ENCHANTABLE;
            case TOOL -> ItemTags.MINING_ENCHANTABLE;
            case ARMOR -> ItemTags.ARMOR_ENCHANTABLE;
            case BOW -> ItemTags.BOW_ENCHANTABLE;
            case TRIDENT -> ItemTags.TRIDENT_ENCHANTABLE;
            case CROSSBOW -> ItemTags.CROSSBOW_ENCHANTABLE;
            case WEARABLE -> ItemTags.EQUIPPABLE_ENCHANTABLE;
            case BREAKABLE -> ItemTags.DURABILITY_ENCHANTABLE;
            case ARMOR_FEET -> ItemTags.FOOT_ARMOR_ENCHANTABLE;
            case ARMOR_HEAD -> ItemTags.HEAD_ARMOR_ENCHANTABLE;
            case ARMOR_LEGS -> ItemTags.LEG_ARMOR_ENCHANTABLE;
            case ARMOR_TORSO -> ItemTags.CHEST_ARMOR_ENCHANTABLE;
            case VANISHABLE -> ItemTags.VANISHING_ENCHANTABLE;
            case FISHING_ROD -> ItemTags.FISHING_ENCHANTABLE;
            default -> throw new IllegalStateException("Unexpected value: " + data.getItemTarget());
        };
    }


    public static net.minecraft.world.entity.EquipmentSlot[] nmsSlots(EnchantInfo enchantment) {

        List<EquipmentSlot> temp = new ArrayList<>();
        EnchantmentTarget itemTarget = enchantment.getItemTarget() == null ? EnchantmentTarget.BREAKABLE : enchantment.getItemTarget();
        if (itemTarget == EnchantmentTarget.ALL) itemTarget = EnchantmentTarget.BREAKABLE;
        switch (itemTarget) {
            case ARMOR:
                temp.add(EquipmentSlot.FEET);
                temp.add(EquipmentSlot.LEGS);
                temp.add(EquipmentSlot.CHEST);
                temp.add(EquipmentSlot.HEAD);
                break;
            case ARMOR_FEET:
                temp.add(EquipmentSlot.FEET);
                break;
            case ARMOR_LEGS:
                temp.add(EquipmentSlot.LEGS);
                break;
            case ARMOR_TORSO:
                temp.add(EquipmentSlot.CHEST);
                break;
            case ARMOR_HEAD:
                temp.add(EquipmentSlot.HEAD);
                break;
            case WEAPON:
                temp.add(EquipmentSlot.HAND);
                temp.add(EquipmentSlot.OFF_HAND);
                break;
            case TOOL:
                temp.add(EquipmentSlot.HAND);
                break;
            case BOW:
                temp.add(EquipmentSlot.HAND);
                break;
            case FISHING_ROD:
                temp.add(EquipmentSlot.HAND);
                break;
            case BREAKABLE:
                temp.add(EquipmentSlot.HAND);
                temp.add(EquipmentSlot.OFF_HAND);
                temp.add(EquipmentSlot.FEET);
                temp.add(EquipmentSlot.LEGS);
                temp.add(EquipmentSlot.CHEST);
                temp.add(EquipmentSlot.HEAD);
                break;
            case WEARABLE:
                temp.add(EquipmentSlot.FEET);
                temp.add(EquipmentSlot.LEGS);
                temp.add(EquipmentSlot.CHEST);
                temp.add(EquipmentSlot.HEAD);
                break;
            case TRIDENT:
                temp.add(EquipmentSlot.HAND);
                break;
            case CROSSBOW:
                temp.add(EquipmentSlot.HAND);
                break;
            case VANISHABLE:
                temp.add(EquipmentSlot.HAND);
                temp.add(EquipmentSlot.OFF_HAND);
                temp.add(EquipmentSlot.FEET);
                temp.add(EquipmentSlot.LEGS);
                temp.add(EquipmentSlot.CHEST);
                temp.add(EquipmentSlot.HEAD);
                break;
        }

        EquipmentSlot[] slots = temp.toArray(new EquipmentSlot[0]);

        net.minecraft.world.entity.EquipmentSlot[] nmsSlots = new net.minecraft.world.entity.EquipmentSlot[slots.length];
        for (int index = 0; index < nmsSlots.length; index++) {
            EquipmentSlot bukkitSlot = slots[index];
            nmsSlots[index] = CraftEquipmentSlot.getNMS(bukkitSlot);
        }
        return nmsSlots;
    }

}
