package me.fullpage.nmslib.v1_19_r2;

import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.craftbukkit.v1_19_R2.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftLivingEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

public final class NMSLib_V1_19_R2 implements NMSHandler {

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        player.spigot().sendMessage(ComponentSerializer.parse(json));
    }


    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        return event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 20, 10);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void clearTitle(Player player) {
        player.resetTitle();
    }

    @Override
    public ItemStack getItemInMainHand(Player player) {
        return player == null ? null : player.getInventory().getItemInMainHand();
    }


    @Override
    public Enchantment lookupEnchantment(String name, int internalId) {
        for (Enchantment value : Enchantment.values()) {
            if (value == null) continue;
            NamespacedKey key = value.getKey();
            if (key.getKey().equals(name) || name.equals(key.getNamespace() + ":" + key.getKey())) {
                return value;
            }
        }
        return null;
    }


    @Override
    public org.bukkit.enchantments.Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        return new org.bukkit.enchantments.Enchantment(new NamespacedKey(plugin, enchantInfo.getName())) {
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
        };
    }


    @Override
    public Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        Enchantment e = lookupEnchantment(enchantInfo.getName(), enchantInfo.getInternalId());
        if (e != null) {
            return e;
        }
        try {
            Enchantment enchantment = buildEnchantment(enchantInfo, plugin);
            registerEnchantment(enchantment);
            return enchantment;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean isRegistered(String name, int internalId) {
        return lookupEnchantment(name, internalId) != null;
    }

    @Override
    public boolean registerEnchantment(org.bukkit.enchantments.Enchantment enchantment) {
        try {
            Field f = org.bukkit.enchantments.Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
            f.setAccessible(false);
            CraftEnchantment.registerEnchantment(enchantment);
            f.setAccessible(true);
            f.set(null, false);
            f.setAccessible(false);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isRegistered(org.bukkit.enchantments.Enchantment enchantment) {
        for (org.bukkit.enchantments.Enchantment value : Enchantment.values()) {
            if (value.equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isGrown(Block block, org.bukkit.block.BlockState blockState) {
        if (block == null) {
            return true;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        BlockData blockData = blockState.getBlockData();
        if (blockData instanceof CaveVinesPlant) {
            CaveVinesPlant caveVinesPlant = (CaveVinesPlant) blockData;
            return caveVinesPlant.isBerries();
        }
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            return ageable.getAge() >= ageable.getMaximumAge();
        }

        return true;
    }

    @Override
    public void setCropToAdult(Block block, org.bukkit.block.BlockState blockState) {
        if (block == null) {
            return;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        BlockData blockData = blockState.getBlockData();
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            ageable.setAge(ageable.getMaximumAge());
            blockState.setBlockData(ageable);
            blockState.update(true);
        }

        if (blockData instanceof CaveVinesPlant) {
            CaveVinesPlant caveVinesPlant = (CaveVinesPlant) blockData;
            caveVinesPlant.setBerries(true);
            blockState.setBlockData(caveVinesPlant);
            blockState.update(true);
        }
    }

    @Override
    public void setCropToBaby(Block block, BlockState blockState) {
        if (block == null) {
            return;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        BlockData blockData = blockState.getBlockData();
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            ageable.setAge(0);
            blockState.setBlockData(ageable);
            blockState.update(true);
        }

        if (blockData instanceof CaveVinesPlant) {
            CaveVinesPlant caveVinesPlant = (CaveVinesPlant) blockData;
            caveVinesPlant.setBerries(false);
            blockState.setBlockData(caveVinesPlant);
            blockState.update(true);
        }

    }

    @Override
    public void moveTo(LivingEntity entity, Location moveTo, float speed) {
        if (entity == null || moveTo == null) {
            return;
        }

        CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
        EntityLiving handle = craftEntity.getHandle();
        if (!(handle instanceof EntityInsentient entityInsentient)) {
            return;
        }
        entityInsentient.B().a(moveTo.getX(), moveTo.getY(), moveTo.getZ(), speed);
    }

    @Override
    public void stopNavigation(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        // unsure so send to current location...
        moveTo(entity, entity.getLocation(), 1);
    }


    @Override
    public void setBiteTime(PlayerFishEvent event, int ticks) {
        try {
            Field hookEntity = event.getClass().getDeclaredField("hookEntity");
            hookEntity.setAccessible(true);
            Object object = hookEntity.get(event);
            CraftEntity craftEntity = (CraftEntity) object;
            EntityFishingHook entityFishingHook = (EntityFishingHook) craftEntity.getHandle();

            Field fishCatchTime = EntityFishingHook.class.getDeclaredField("as");
            fishCatchTime.setAccessible(true);
            fishCatchTime.setInt(entityFishingHook, Math.max(15, ticks));
            fishCatchTime.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
