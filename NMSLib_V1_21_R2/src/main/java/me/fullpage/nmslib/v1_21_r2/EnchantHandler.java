package me.fullpage.nmslib.v1_21_r2;

import me.fullpage.manticlib.utils.RandomMaterials;
import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.Reflect;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class EnchantHandler {

    private static final MinecraftServer minecraftServer;
    private static final MappedRegistry<Enchantment> enchantRegistery;
    private static final MappedRegistry<Item> itemRegistery;

    private static final String HolderSetNamedContentsField = "c";

    private static final String REGISTRY_FROZEN_TAGS_FIELD = "j"; // frozenTags
    private static final String REGISTRY_ALL_TAGS_FIELD    = "k"; // allTags
    private static final String TAG_SET_UNBOUND_METHOD     = "a"; // .unbound()
    private static final String TAG_SET_MAP_FIELD          = "val$map";

    static {
        minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        enchantRegistery = (MappedRegistry<Enchantment>) minecraftServer.registryAccess().lookup(Registries.ENCHANTMENT).orElseThrow();
        itemRegistery = (MappedRegistry<Item>) minecraftServer.registryAccess().lookup(Registries.ITEM).orElseThrow();
    }

    public static void unfreezeRegistry() {
        unfreeze(enchantRegistery);
        unfreeze(itemRegistery);
    }

    public static void freezeRegistry() {
        freeze(enchantRegistery);
        freeze(itemRegistery);
    }

    private static <T> void unfreeze(@NotNull MappedRegistry<T> registry) {
        Reflect.setFieldValue(registry, "l", false);             // MappedRegistry#frozen
        Reflect.setFieldValue(registry, "m", new IdentityHashMap<>()); // MappedRegistry#unregisteredIntrusiveHolders
    }

    private static <T> void freeze(@NotNull MappedRegistry<T> registry) {
        Object tagSet = getAllTags(registry);

        Map<TagKey<T>, HolderSet.Named<T>> tagsMap = getTagsMap(tagSet);
        Map<TagKey<T>, HolderSet.Named<T>> frozenTags = getFrozenTags(registry);

        tagsMap.forEach(frozenTags::putIfAbsent);

        unbound(registry);

        registry.freeze();

        frozenTags.forEach(tagsMap::putIfAbsent);

        Reflect.setFieldValue(tagSet, TAG_SET_MAP_FIELD, tagsMap);
        Reflect.setFieldValue(registry, REGISTRY_ALL_TAGS_FIELD, tagSet);
    }

    private static <T> void unbound(@NotNull MappedRegistry<T> registry) {
        Class<?> tagSetClass = Reflect.getInnerClass(MappedRegistry.class.getName(), "TagSet");

        Method unboundMethod = Reflect.getMethod(tagSetClass, TAG_SET_UNBOUND_METHOD);
        Object unboundTagSet = Reflect.invokeMethod(unboundMethod, registry); // new TagSet object.

        Reflect.setFieldValue(registry, REGISTRY_ALL_TAGS_FIELD, unboundTagSet);
    }

    public static EquipmentSlotGroup[] nmsSlots(EnchantInfo enchantment) {

        List<EquipmentSlotGroup> temp = new ArrayList<>();
        EnchantmentTarget itemTarget = enchantment.getItemTarget() == null ? EnchantmentTarget.BREAKABLE : enchantment.getItemTarget();
        if (itemTarget == EnchantmentTarget.ALL) {
            return new EquipmentSlotGroup[]{EquipmentSlotGroup.ANY};
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

        EquipmentSlotGroup[] nmsSlots = new EquipmentSlotGroup[slots.length];
        for (int index = 0; index < nmsSlots.length; index++) {
            EquipmentSlot bukkitSlot = slots[index];
            nmsSlots[index] = CraftEquipmentSlot.getNMSGroup(bukkitSlot.getGroup());
        }
        return nmsSlots;
    }


    public static org.bukkit.enchantments.Enchantment registerEnchantment(EnchantInfo data) {

        Component component = CraftChatMessage.fromStringOrEmpty(data.getName().toLowerCase());
        HolderSet.Named<Item> supportedItems = createItemsSet("enchant_supported", data.getName().toLowerCase(), data.getItemTarget());
        HolderSet.Named<Item> primaryItems = createItemsSet("enchant_primary", data.getName().toLowerCase(), data.getItemTarget());
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
        EquipmentSlotGroup[] slots = nmsSlots(data);

        Enchantment.EnchantmentDefinition definition = Enchantment.definition(supportedItems, primaryItems, weight, maxLevel, minCost, maxCost, anvilCost, slots);

        HolderSet<Enchantment> exclusiveSet = createExclusiveSet(data.getName().toLowerCase());

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

        return CraftEnchantment.minecraftToBukkit(enchantment);
    }

    @NotNull
    private static HolderSet.Named<Enchantment> createExclusiveSet(@NotNull String enchantId) {
        TagKey<Enchantment> customKey = getTagKey(enchantRegistery, "exclusive_set/" + enchantId);
        List<Holder<Enchantment>> holders = new ArrayList<>();

        enchantRegistery.bindTag(customKey, holders);

        return getFrozenTags(enchantRegistery).get(customKey);
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
        HolderSet.Named<Enchantment> holders = enchantRegistery.getOrThrow(tagKey); //.getTag(tagKey).orElse(null);
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

    @NotNull
    private static HolderSet.Named<Item> createItemsSet(@NotNull String prefix, @NotNull String enchantId, @NotNull EnchantmentTarget enchantmentTarget) {
        TagKey<Item> customKey = getTagKey(itemRegistery, prefix + "/" + enchantId.toLowerCase());
        List<Holder<Item>> holders = new ArrayList<>();

        List<Material> materials = new ArrayList<>();
        for (Material value : Material.values()) {
            if (enchantmentTarget.includes(value)) {
                materials.add(value);
            }
        }

        materials.forEach(material -> {
            ResourceLocation location = CraftNamespacedKey.toMinecraft(material.getKey());
            Holder.Reference<Item> holder = itemRegistery.get(location).orElse(null);
            if (holder == null) return;

            holders.add(holder);
        });

        itemRegistery.bindTag(customKey, holders);

        return getFrozenTags(itemRegistery).get(customKey);
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

    private static <T> TagKey<T> getTagKey(@NotNull Registry<T> registry, @NotNull String name) {
        return TagKey.create(registry.key(), ResourceLocation.withDefaultNamespace(name));
    }

    @NotNull
    private static <T> Map<TagKey<T>, HolderSet.Named<T>> getFrozenTags(@NotNull MappedRegistry<T> registry) {
        return (Map<TagKey<T>, HolderSet.Named<T>>) Reflect.getFieldValue(registry, REGISTRY_FROZEN_TAGS_FIELD);
    }

    @NotNull
    private static <T> Object getAllTags(@NotNull MappedRegistry<T> registry) {
        return Reflect.getFieldValue(registry, REGISTRY_ALL_TAGS_FIELD);
    }

    @NotNull
    private static <T> Map<TagKey<T>, HolderSet.Named<T>> getTagsMap(@NotNull Object tagSet) {
        // new HashMap, because original is ImmutableMap.
        return new HashMap<>((Map<TagKey<T>, HolderSet.Named<T>>) Reflect.getFieldValue(tagSet, TAG_SET_MAP_FIELD));
    }
}
