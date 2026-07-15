package me.fullpage.nmslib.v1_21_r5;

import me.fullpage.manticlib.utils.RandomMaterials;
import me.fullpage.nmslib.EnchantInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
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
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class EnchantHandler {

    private static final MinecraftServer minecraftServer;
    private static final MappedRegistry<Enchantment> enchantRegistery;
    private static final MappedRegistry<Item> itemRegistery;
    private static final String REGISTRY_FROZEN_TAGS_FIELD = "frozenTags";
    private static final String REGISTRY_ALL_TAGS_FIELD = "allTags";
    private static final String REGISTRY_FROZEN_FIELD = "frozen";
    private static final String REGISTRY_UNREGISTERED_INTRUSIVE_HOLDERS_FIELD = "unregisteredIntrusiveHolders";

    private static volatile String tagSetMapFieldCache;
    private static volatile String tagSetUnboundMethodCache;

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
        setSuperFieldValue(registry, MappedRegistry.class, REGISTRY_FROZEN_FIELD, false);
        setSuperFieldValue(registry, MappedRegistry.class, REGISTRY_UNREGISTERED_INTRUSIVE_HOLDERS_FIELD, new IdentityHashMap<>());
        Object frozenValueAfter = getSuperFieldValue(registry, MappedRegistry.class, REGISTRY_FROZEN_FIELD);
    }

    private static void setSuperFieldValue(@NotNull Object instance, @NotNull Class<?> declaringClass, @NotNull String fieldName, Object value) {
        try {
            Field field = findFieldInHierarchy(instance.getClass(), fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on " + declaringClass.getName(), e);
        }
    }

    @NotNull
    private static Object getSuperFieldValue(@NotNull Object instance, @NotNull Class<?> declaringClass, @NotNull String fieldName) {
        try {
            Field field = findFieldInHierarchy(instance.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get field '" + fieldName + "' on " + declaringClass.getName(), e);
        }
    }

    private static Field findFieldInHierarchy(Class<?> startClass, String fieldName) throws NoSuchFieldException {
        Class<?> current = startClass;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static <T> void freeze(@NotNull MappedRegistry<T> registry) {
        Object tagSet = getAllTags(registry);
        Map<TagKey<T>, HolderSet.Named<T>> tagsMap = getTagsMap(tagSet);
        Map<TagKey<T>, HolderSet.Named<T>> frozenTags = getFrozenTags(registry);
        tagsMap.forEach(frozenTags::putIfAbsent);
        unbound(registry);
        registry.freeze();
        frozenTags.forEach(tagsMap::putIfAbsent);
        Field tagSetMapField = findTagSetMapField(tagSet);
        try {
            tagSetMapField.set(tagSet, tagsMap);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set TagSet map field '" + tagSetMapField.getName() + "'", e);
        }
        setSuperFieldValue(registry, MappedRegistry.class, REGISTRY_ALL_TAGS_FIELD, tagSet);
    }

    private static <T> void unbound(@NotNull MappedRegistry<T> registry) {
        Field allTagsField;
        try {
            allTagsField = findFieldInHierarchy(registry.getClass(), REGISTRY_ALL_TAGS_FIELD);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to locate allTags field '" + REGISTRY_ALL_TAGS_FIELD + "' on " + registry.getClass().getName(), e);
        }
        Class<?> tagSetClass = allTagsField.getType();

        Method unboundMethod = findTagSetUnboundMethod(tagSetClass);
        Object unboundTagSet;
        try {
            unboundMethod.setAccessible(true);
            unboundTagSet = unboundMethod.invoke(null); // static factory method, no target instance
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke TagSet 'unbound' factory method on " + tagSetClass.getName(), e);
        }

        setSuperFieldValue(registry, MappedRegistry.class, REGISTRY_ALL_TAGS_FIELD, unboundTagSet);
    }

    private static Method findTagSetUnboundMethod(Class<?> tagSetClass) {
        if (tagSetUnboundMethodCache != null) {
            try {
                Method cached = tagSetClass.getDeclaredMethod(tagSetUnboundMethodCache);
                cached.setAccessible(true);
                return cached;
            } catch (NoSuchMethodException ignored) {
                // fall through and rediscover
            }
        }
        for (Method m : tagSetClass.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 0
                    && tagSetClass.isAssignableFrom(m.getReturnType())) {
                tagSetUnboundMethodCache = m.getName();
                m.setAccessible(true);
                return m;
            }
        }
        throw new IllegalStateException("TagSet 'unbound' factory method not found on " + tagSetClass.getName());
    }

    private static Field findTagSetMapField(Object tagSetInstance) {
        Class<?> tagSetClass = tagSetInstance.getClass();
        if (tagSetMapFieldCache != null) {
            try {
                Field cached = findFieldInHierarchy(tagSetClass, tagSetMapFieldCache);
                cached.setAccessible(true);
                if (cached.get(tagSetInstance) != null) {
                    return cached;
                }
            } catch (ReflectiveOperationException ignored) {
                // fall through and rediscover
            }
        }
        Class<?> current = tagSetClass;
        StringBuilder diagnostic = new StringBuilder();
        while (current != null) {
            diagnostic.append("  ").append(current.getName()).append(":\n");
            for (Field f : current.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object value;
                    try {
                        value = f.get(tagSetInstance);
                    } catch (IllegalAccessException e) {
                        value = "<inaccessible>";
                    }
                    diagnostic.append("    Map ").append(f.getName()).append(" = ")
                            .append(value == null ? "null" : "non-null").append('\n');
                    if (value != null) {
                        tagSetMapFieldCache = f.getName();
                        return f;
                    }
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Non-null TagSet map field not found on " + tagSetClass.getName());
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
        int anvilCost = 1;
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

        return CraftEnchantment.minecraftHolderToBukkit(reference);
    }

    private static TagKey<Item> customItemsTag(String path) {
        return TagKey.create(itemRegistery.key(), customIdentifier(path));
    }

    private static HolderSet.Named<Enchantment> createExclusiveSet(String id) {
        TagKey<Enchantment> customKey = customTagKey(enchantRegistery, "exclusive_set/" + id);
        List<Holder<Enchantment>> holders = new ArrayList<>();

        // Creates the tag, puts it in the frozenTags map, and binds the holder list to it.
        enchantRegistery.bindTag(customKey, holders);

        return getFrozenTags(enchantRegistery).get(customKey);
    }

    private static <T> TagKey<T> customTagKey(Registry<T> registry, String name) {
        return TagKey.create(registry.key(), customIdentifier(name));
    }

    private static ResourceLocation customIdentifier(String value) {
        return CraftNamespacedKey.toMinecraft(NamespacedKey.minecraft(value));
    }

    private static <T> HolderSet.Named<T> bindTag(MappedRegistry<T> registry, TagKey<T> tagKey, List<Holder<T>> holders) {
        registry.bindTag(tagKey, holders);
        return getFrozenTags(registry).get(tagKey);
    }

    private static void addInTag(TagKey<Enchantment> tagKey, Holder.Reference<Enchantment> reference) {
        modifyTag(tagKey, reference, (contents, holder) -> {
            if (!contents.contains(holder)) contents.add(holder);
        });
    }

    private static void removeFromTag(TagKey<Enchantment> tagKey, Holder.Reference<Enchantment> reference) {
        modifyTag(tagKey, reference, List::remove);
    }

    private static void modifyTag(TagKey<Enchantment> tagKey,
                                  Holder.Reference<Enchantment> reference,
                                  BiConsumer<List<Holder<Enchantment>>, Holder.Reference<Enchantment>> consumer) {
        HolderSet.Named<Enchantment> holders = enchantRegistery.get(tagKey).orElse(null);
        if (holders == null) {
            return;
        }

        modifyHolderSetContents(enchantRegistery, tagKey, holders, reference, consumer);
    }

    @SuppressWarnings("unchecked")
    private static <T> void modifyHolderSetContents(MappedRegistry<T> registry,
                                                    TagKey<T> tagKey,
                                                    HolderSet.Named<T> holders,
                                                    Holder.Reference<T> reference,
                                                    BiConsumer<List<Holder<T>>, Holder.Reference<T>> consumer) {

        List<Holder<T>> contents = new ArrayList<>(holders.stream().toList());
        consumer.accept(contents, reference);

        bindTag(registry, tagKey, contents);
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
        return bindTag(itemRegistery, customKey, holders);
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

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T> Map<TagKey<T>, HolderSet.Named<T>> getFrozenTags(@NotNull MappedRegistry<T> registry) {
        Object value = getSuperFieldValue(registry, MappedRegistry.class, REGISTRY_FROZEN_TAGS_FIELD);
        if (value == null) {
            Map<TagKey<T>, HolderSet.Named<T>> fresh = new IdentityHashMap<>();
            setSuperFieldValue(registry, MappedRegistry.class, REGISTRY_FROZEN_TAGS_FIELD, fresh);
            return fresh;
        }
        return (Map<TagKey<T>, HolderSet.Named<T>>) value;
    }

    @NotNull
    private static <T> Object getAllTags(@NotNull MappedRegistry<T> registry) {
        return getSuperFieldValue(registry, MappedRegistry.class, REGISTRY_ALL_TAGS_FIELD);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T> Map<TagKey<T>, HolderSet.Named<T>> getTagsMap(@NotNull Object tagSet) {
        Field mapField = findTagSetMapField(tagSet);
        try {
            Object rawValue = mapField.get(tagSet);
            return new HashMap<>((Map<TagKey<T>, HolderSet.Named<T>>) rawValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read TagSet map field '" + mapField.getName() + "'", e);
        }
    }
}