package me.fullpage.nmslib.v26_2;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Player inventory scans on the vanilla stacks, read the way Bukkit's item mirrors and persistent data views read them, without creating either. */
final class InventorySlots {
    private InventorySlots() { }

    // NamespacedKey.toString builds a new string on every call; callers pass a few long-lived keys, so this stays small.
    private static final Map<NamespacedKey, String> NAMES = new ConcurrentHashMap<>();

    static long dataSlots(PlayerInventory inventory, NamespacedKey key) {
        Inventory handle = ((CraftInventoryPlayer) inventory).getInventory();
        String name = name(key);
        long slots = 0;
        // getContents order is slot order: the 36 storage slots, then equipment sorted by slot index.
        for (int slot = 0, size = handle.getContainerSize(); slot < size; slot++) {
            if (data(handle.getItem(slot), name) != null) slots |= 1L << slot;
        }
        return slots;
    }

    static int dataSlot(PlayerInventory inventory, NamespacedKey key, String value, int hint) {
        Inventory handle = ((CraftInventoryPlayer) inventory).getInventory();
        String name = name(key);
        int size = handle.getContainerSize();
        if (hint >= 0 && hint < size && data(handle.getItem(hint), name) instanceof StringTag(String found) && found.equals(value)) return hint;
        for (int slot = 0; slot < size; slot++) {
            if (slot != hint && data(handle.getItem(slot), name) instanceof StringTag(String found) && found.equals(value)) return slot;
        }
        return -1;
    }

    static boolean hasStorageRoom(PlayerInventory inventory) {
        Inventory handle = ((CraftInventoryPlayer) inventory).getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = handle.getItem(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }

    static long addToStorage(PlayerInventory inventory, org.bukkit.inventory.ItemStack prototype, long amount, int maxStack) {
        Inventory handle = ((CraftInventoryPlayer) inventory).getInventory();
        ItemStack like = CraftItemStack.unwrap(prototype);
        long empty = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && amount > 0; slot++) {
            ItemStack stack = handle.getItem(slot);
            if (stack.isEmpty()) {
                empty |= 1L << slot;
                continue;
            }
            int take = ItemStack.isSameItemSameComponents(stack, like) ? (int) Math.min(amount, Math.max(0, maxStack - stack.getCount())) : 0;
            if (take == 0) continue;
            stack.grow(take);
            amount -= take;
        }
        for (; empty != 0 && amount > 0; empty &= empty - 1) {
            int take = (int) Math.min(amount, maxStack);
            org.bukkit.inventory.ItemStack stack = prototype.clone();
            stack.setAmount(take);
            // setItem sends the slot right away, as a new stack in an empty slot always has.
            inventory.setItem(Long.numberOfTrailingZeros(empty), stack);
            amount -= take;
        }
        return amount;
    }

    // get first: computeIfAbsent locks whenever the key is not first in its bin, and the two plugin keys share one.
    private static String name(NamespacedKey key) {
        String name = NAMES.get(key);
        return name != null ? name : NAMES.computeIfAbsent(key, NamespacedKey::toString);
    }

    /** The item's entry under PublicBukkitValues, null for empty items like Bukkit's null slots. */
    @SuppressWarnings("deprecation") // getUnsafe is only read, as CraftItemStack's view does
    private static Tag data(ItemStack stack, String name) {
        if (stack.isEmpty()) return null;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null && custom.getUnsafe().get(CraftItemStack.PDC_CUSTOM_DATA_KEY) instanceof CompoundTag values ? values.get(name) : null;
    }
}
