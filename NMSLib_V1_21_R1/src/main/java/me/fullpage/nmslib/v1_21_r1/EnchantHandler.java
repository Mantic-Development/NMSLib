package me.fullpage.nmslib.v1_21_r1;

import me.fullpage.manticlib.utils.RandomMaterials;
import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.Reflect;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R1.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

public class EnchantHandler {

    private static final MinecraftServer minecraftServer;
    private static final Registry<Enchantment> enchantRegistery;

    private static final String HolderSetNamedContentsField = "c";
    private static final String holderReferenceTagField = "b";

    static {
        minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        enchantRegistery = minecraftServer.registryAccess().registry(Registries.ENCHANTMENT).orElse(null);
    }

    public static void unfreezeRegistry() {
        Reflect.setFieldValue(enchantRegistery, "l", false);
        Reflect.setFieldValue(enchantRegistery, "m", new IdentityHashMap<>());
    }

    public static void freezeRegistry() {
        enchantRegistery.freeze();
    }

    public static net.minecraft.world.entity.EquipmentSlotGroup[] nmsSlots(EnchantInfo enchantment) {

        List<EquipmentSlotGroup> temp = new ArrayList<>();
        EnchantmentTarget itemTarget = enchantment.getItemTarget() == null ? EnchantmentTarget.BREAKABLE : enchantment.getItemTarget();
        if (itemTarget == EnchantmentTarget.ALL) {
            return new net.minecraft.world.entity.EquipmentSlotGroup[]{EquipmentSlotGroup.ANY};
        }
        switch (itemTarget) {
            case ARMOR:
                temp.add(EquipmentSlotGroup.FEET);
                temp.add(EquipmentSlotGroup.LEGS);
                temp.add(EquipmentSlotGroup.CHEST);
                temp.add(EquipmentSlotGroup.HEAD);
                break;
            case ARMOR_FEET:
                temp.add(EquipmentSlotGroup.FEET);
                break;
            case ARMOR_LEGS:
                temp.add(EquipmentSlotGroup.LEGS);
                break;
            case ARMOR_TORSO:
                temp.add(EquipmentSlotGroup.CHEST);
                break;
            case ARMOR_HEAD:
                temp.add(EquipmentSlotGroup.HEAD);
                break;
            case WEAPON:
                temp.add(EquipmentSlotGroup.MAINHAND);
                temp.add(EquipmentSlotGroup.OFFHAND);
                break;
            case TOOL:
                temp.add(EquipmentSlotGroup.MAINHAND);
                break;
            case BOW:
                temp.add(EquipmentSlotGroup.MAINHAND);
                break;
            case FISHING_ROD:
                temp.add(EquipmentSlotGroup.MAINHAND);
                break;
            case BREAKABLE:
                temp.add(EquipmentSlotGroup.MAINHAND);
                temp.add(EquipmentSlotGroup.OFFHAND);
                temp.add(EquipmentSlotGroup.FEET);
                temp.add(EquipmentSlotGroup.LEGS);
                temp.add(EquipmentSlotGroup.CHEST);
                temp.add(EquipmentSlotGroup.HEAD);
                break;
            case WEARABLE:
                temp.add(EquipmentSlotGroup.FEET);
                temp.add(EquipmentSlotGroup.LEGS);
                temp.add(EquipmentSlotGroup.CHEST);
                temp.add(EquipmentSlotGroup.HEAD);
                break;
            case TRIDENT:
                temp.add(EquipmentSlotGroup.MAINHAND);
                break;
            case CROSSBOW:
                temp.add(EquipmentSlotGroup.MAINHAND);
                break;
            case VANISHABLE:
                temp.add(EquipmentSlotGroup.MAINHAND);
                temp.add(EquipmentSlotGroup.OFFHAND);
                temp.add(EquipmentSlotGroup.FEET);
                temp.add(EquipmentSlotGroup.LEGS);
                temp.add(EquipmentSlotGroup.CHEST);
                temp.add(EquipmentSlotGroup.HEAD);
                break;
        }

        EquipmentSlot[] slots = temp.toArray(new EquipmentSlot[0]);

        net.minecraft.world.entity.EquipmentSlotGroup[] nmsSlots = new net.minecraft.world.entity.EquipmentSlotGroup[slots.length];
        for (int index = 0; index < nmsSlots.length; index++) {
            EquipmentSlot bukkitSlot = slots[index];
            nmsSlots[index] = CraftEquipmentSlot.getNMSGroup(bukkitSlot.getGroup());
        }
        return nmsSlots;
    }


    public static org.bukkit.enchantments.Enchantment registerEnchantment(EnchantInfo data) {

        Component component = CraftChatMessage.fromStringOrEmpty(data.getName().toLowerCase());
        HolderSet.Named<Item> supportedItems = createItemSet("enchant_supported", data, data.getItemTarget());
        HolderSet.Named<Item> primaryItems = createItemSet("enchant_primary", data, data.getItemTarget());
        int weight = 10;
        /*
        * COMMON(10),
          UNCOMMON(5),
          RARE(2),
          VERY_RARE(1);
        * */
        int maxLevel = data.getMaxLevel();
        Enchantment.Cost minCost = new Enchantment.Cost(1, 0);
        Enchantment.Cost maxCost = new Enchantment.Cost(1, 0);
        int anvilCost = 10;
        net.minecraft.world.entity.EquipmentSlotGroup[] slots = nmsSlots(data);

        Enchantment.EnchantmentDefinition definition = Enchantment.definition(supportedItems, primaryItems, weight, maxLevel, minCost, maxCost, anvilCost, slots);
        HolderSet<Enchantment> exclusiveSet = HolderSet.direct();

        Enchantment enchantment = new Enchantment(component, definition, exclusiveSet, DataComponentMap.builder().build());

        Holder.Reference<Enchantment> reference = enchantRegistery.createIntrusiveHolder(enchantment);
        Registry.register(enchantRegistery, data.getName().toLowerCase(), enchantment);

        if (data.isCursed()) {
            addInTag(EnchantmentTags.CURSE, reference);
        } else {
            if (data.isTreasure()) {
                addInTag(EnchantmentTags.TREASURE, reference);
            } else addInTag(EnchantmentTags.NON_TREASURE, reference);

            removeFromTag(EnchantmentTags.TRADEABLE, reference); // prevent trading
            removeFromTag(EnchantmentTags.IN_ENCHANTING_TABLE, reference); // prevent enchanting
        }

        org.bukkit.enchantments.Enchantment bukkitEnchant = CraftEnchantment.minecraftToBukkit(enchantment);
        return bukkitEnchant;
    }


    private static void addInTag(TagKey<Enchantment> tagKey, Holder.Reference<Enchantment> reference) {
        modfiyTag(tagKey, reference, List::add);
    }

    private static void removeFromTag(TagKey<Enchantment> tagKey, Holder.Reference<Enchantment> reference) {
        modfiyTag(tagKey, reference, List::remove);
    }

    private static void modfiyTag(TagKey<Enchantment> tagKey,
                                  Holder.Reference<Enchantment> reference,
                                  BiConsumer<List<Holder<Enchantment>>, Holder.Reference<Enchantment>> consumer) {
        HolderSet.Named<Enchantment> holders = enchantRegistery.getTag(tagKey).orElse(null);
        if (holders == null) {
            Bukkit.getLogger().warning(tagKey + ": Could not modify HolderSet. HolderSet is null.");
            return;
        }

        modifyHolderSetContents(holders, reference, consumer);
    }

    @SuppressWarnings("unchecked")
    private static <T> void modifyHolderSetContents(HolderSet.Named<T> holders,
                                                    Holder.Reference<T> reference,
                                                    BiConsumer<List<Holder<T>>, Holder.Reference<T>> consumer) {

        List<Holder<T>> contents = new ArrayList<>((List<Holder<T>>) Reflect.getFieldValue(holders, HolderSetNamedContentsField));
        consumer.accept(contents, reference);
        Reflect.setFieldValue(holders, HolderSetNamedContentsField, contents);
    }

    private static HolderSet.Named<Item> createItemSet(String prefix, EnchantInfo data, EnchantmentTarget enchantmentTarget) {
        Registry<Item> items = minecraftServer.registryAccess().registry(Registries.ITEM).orElseThrow();
        TagKey<Item> customKey = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(prefix + "/" + data.getName().toLowerCase()));
        HolderSet.Named<Item> customItems = items.getOrCreateTag(customKey);
        List<Holder<Item>> holders = new ArrayList<>();

        List<Material> materials = new ArrayList<>();
        for (Material value : Material.values()) {
            if (enchantmentTarget.includes(value)) {
                materials.add(value);
            }
        }

        materials.forEach(material -> {
            ResourceLocation location = CraftNamespacedKey.toMinecraft(material.getKey());
            Holder.Reference<Item> holder = items.getHolder(location).orElse(null);
            if (holder == null) return;

            Set<TagKey<Item>> holderTags = new HashSet<>((Set<TagKey<Item>>) Reflect.getFieldValue(holder, holderReferenceTagField));
            holderTags.add(customKey);
            Reflect.setFieldValue(holder, holderReferenceTagField, holderTags);

            holders.add(holder);
        });

        Reflect.setFieldValue(customItems, HolderSetNamedContentsField, holders);

        return customItems;
    }

    private static Set<Material> getItemsBySlot(@NotNull EquipmentSlot slot) {
        Set<Material> materials = new HashSet<>();
        RandomMaterials.getAll().forEach(material -> {
            if (material.isItem() && material.getEquipmentSlot() == slot) {
                materials.add(material);
            }
        });
        return materials;
    }


}
