package me.fullpage.nmslib.v1_20_r3;

import me.fullpage.nmslib.EnchantInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentSlotType;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R3.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_20_R3.block.impl.CraftTarget;
import org.bukkit.craftbukkit.v1_20_R3.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftEntityEquipment;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public class ManticServerEnchant extends Enchantment {

    private final EnchantInfo enchant;
    public ManticServerEnchant(EnchantInfo enchant) {
        super(Rarity.d, EnchantmentSlotType.e, nmsSlots(enchant));
        this.enchant = enchant;
    }

   /* public static EnchantmentSlotType nmsCategory( ManticApiEnchant enchantment) {
  return EnchantmentSlotType.n;
    }*/
   public boolean b() {
       return false;
   }

    public boolean c() {
        return false;
    }

    public boolean h() {
        return false;
    }

    public boolean i() {
        return false;
    }

    @Override
    public int e() {
        return enchant.getStartLevel();
    }

    @Override
    public int a() {
        return enchant.getMaxLevel();
    }

    public int a(int level) {
        return 1 + level * 10;
    }

    public int b(int level) {
        return a(level) + 5;
    }

    protected boolean a(Enchantment other) {
       return enchant.conflictsWith(CraftEnchantment.minecraftToBukkit(other));
    }

    public boolean a(ItemStack item) {
        return super.a(item);// todo
    }

    public static EnumItemSlot[] nmsSlots(EnchantInfo enchantment) {

        List<EquipmentSlot> temp = new ArrayList<>();
        EnchantmentTarget itemTarget = enchantment.getItemTarget() == null ? EnchantmentTarget.ALL : enchantment.getItemTarget() ;
        switch (itemTarget) {
            case ALL:
                temp.add(EquipmentSlot.HAND);
                temp.add(EquipmentSlot.OFF_HAND);
                temp.add(EquipmentSlot.FEET);
                temp.add(EquipmentSlot.LEGS);
                temp.add(EquipmentSlot.CHEST);
                temp.add(EquipmentSlot.HEAD);
                break;
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

        EquipmentSlot[] slots = temp.toArray(new EquipmentSlot[temp.size()]);

        EnumItemSlot[] nmsSlots = new EnumItemSlot[slots.length];
        for (int index = 0; index < nmsSlots.length; index++) {
            EquipmentSlot bukkitSlot = slots[index];
            nmsSlots[index] = CraftEquipmentSlot.getNMS(bukkitSlot);
        }
        return nmsSlots;
    }

}
