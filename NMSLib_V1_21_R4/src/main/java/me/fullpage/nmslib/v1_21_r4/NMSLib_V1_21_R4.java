package me.fullpage.nmslib.v1_21_r4;

import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.projectile.FishingHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;

public final class NMSLib_V1_21_R4 implements NMSHandler {

    public NMSLib_V1_21_R4() {
        ((CraftMagicNumbers) CraftMagicNumbers.INSTANCE).getMappingsVersion();
    }

    @Override
    public int spawnPacketHologram(Player player, Location location, String text) {
        if (player == null || location == null || location.getWorld() == null || text == null
                || !player.isOnline() || player.getWorld() != location.getWorld()) {
            return -1;
        }
        net.minecraft.server.level.ServerPlayer nmsPlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
        net.minecraft.world.entity.decoration.ArmorStand stand =
                new net.minecraft.world.entity.decoration.ArmorStand(
                        nmsPlayer.level(), location.getX(), location.getY() - 0.5, location.getZ());
        org.bukkit.entity.ArmorStand display = (org.bukkit.entity.ArmorStand) stand.getBukkitEntity();
        display.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
        display.setCustomName(text);
        display.setCustomNameVisible(true);
        display.setGravity(false);
        display.setMarker(true);

        nmsPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(
                display.getEntityId(), stand.getUUID(), location.getX(), location.getY() - 0.5, location.getZ(),
                0.0f, 0.0f, stand.getType(), 0,
                net.minecraft.world.phys.Vec3.ZERO, 0.0));
        nmsPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                display.getEntityId(), stand.getEntityData().getNonDefaultValues()));
        return display.getEntityId();
    }

    @Override
    public void removePacketEntities(Player player, int... entityIds) {
        if (player == null || !player.isOnline() || entityIds == null || entityIds.length == 0) {
            return;
        }
        ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle().connection.send(
                new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(entityIds));
    }

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
    public ItemStack getItemInUse(Player player) {
        return player == null ? null : player.getItemInUse();
    }

    @Override
    public Enchantment lookupEnchantment(String name, int internalId) {
        for (Enchantment value : Enchantment.values()) {
            if (value == null) continue;
            NamespacedKey key = value.getKey();
            if (key.getKey().equalsIgnoreCase(name) || name.equalsIgnoreCase(key.getNamespace() + ":" + key.getKey())) {
                return value;
            }
        }
        return null;
    }


    @Override
    public org.bukkit.enchantments.Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, enchantInfo.getName());
        return new ManticApiEnchant(key, enchantInfo);
    }


    @Override
    public Enchantment registerEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        Enchantment enchantment = lookupEnchantment(enchantInfo.getName(), enchantInfo.getInternalId());
        if (enchantment != null) {
            return enchantment;
        }
        EnchantHandler.unfreezeRegistry();

        Enchantment ench = EnchantHandler.registerEnchantment(enchantInfo);
        EnchantHandler.freezeRegistry();
        return ench;
    }

    @Override
    public HashMap<EnchantInfo, Enchantment> registerEnchantments(Collection<EnchantInfo> enchantInfos, Plugin plugin) {
        HashMap<EnchantInfo, Enchantment> temp = new HashMap<>();
        for (EnchantInfo enchantInfo : enchantInfos) {
            Enchantment enchantment = registerEnchantment(enchantInfo, plugin);
            temp.put(enchantInfo, enchantment);
        }
        return temp;
    }

    @Override
    public boolean registerEnchantment(org.bukkit.enchantments.Enchantment enchantment) {
        throw new UnsupportedOperationException("This method is not supported in 1.20.4 and above. Use registerEnchantment(EnchantInfo, Plugin) instead.");

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
    public boolean isRegistered(String name, int internalId) {
        return lookupEnchantment(name, internalId) != null;
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
        net.minecraft.world.entity.LivingEntity handle = craftEntity.getHandle();

        if (!(handle instanceof PathfinderMob pathfinderMob)) {
            return;
        }
        pathfinderMob.getNavigation().moveTo(moveTo.getX(), moveTo.getY(), moveTo.getZ(), speed);
    }


    @Override
    public void stopNavigation(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
        net.minecraft.world.entity.LivingEntity handle = craftEntity.getHandle();
        if (!(handle instanceof PathfinderMob pathfinderMob)) {
            return;
        }
        PathNavigation navigation = pathfinderMob.getNavigation();
        navigation.stop();
    }


    @Override
    public void setBiteTime(PlayerFishEvent event, int ticks) {
        try {
            Field hookEntity = event.getClass().getDeclaredField("hookEntity");
            hookEntity.setAccessible(true);
            Object object = hookEntity.get(event);
            CraftEntity craftEntity = (CraftEntity) object;
            FishingHook entityFishingHook = (FishingHook) craftEntity.getHandle();

            Field fishCatchTime = FishingHook.class.getDeclaredField("l"); // ignore cannot resolve, it will be remapped
            fishCatchTime.setAccessible(true);
            fishCatchTime.setInt(entityFishingHook, Math.max(15, ticks));
            fishCatchTime.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void simulateRodInteraction(Player player) {
        ServerPlayer p = ((CraftPlayer) player).getHandle();

        InteractionHand hand = getHand(player);
        p.gameMode.useItem(p, p.level(), p.getItemInHand(hand), hand);

        p.swing(hand, true);
    }

    private InteractionHand getHand(Player player) {
        return getItemInMainHand(player).getType().equals(Material.FISHING_ROD) ?
                InteractionHand.MAIN_HAND : player.getInventory().getItemInOffHand().getType().equals(Material.FISHING_ROD) ?
                                            InteractionHand.OFF_HAND : null;
    }

    @Override
    public boolean isInWater(Entity entity) {
        return entity.isInWater();
    }

    @Override
    public void setVelocity(Entity entity, Vector vector) {
        entity.setVelocity(vector);
    }

    @Override
    public Vector getVelocity(Entity entity) {
        return entity.getVelocity();
    }

    @Override
    public void setStairDirection(BlockState blockState, BlockFace blockFace, boolean inverted) {
        Stairs stairs = (Stairs) blockState.getBlockData();
        stairs.setFacing(blockFace);
        blockState.setBlockData(stairs);
    }
}