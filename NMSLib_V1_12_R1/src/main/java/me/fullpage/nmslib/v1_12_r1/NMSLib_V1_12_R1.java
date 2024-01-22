package me.fullpage.nmslib.v1_12_r1;

import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketPlayOutChat;
import net.minecraft.server.v1_12_R1.PacketPlayOutTitle;
import org.bukkit.CropState;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_12_R1.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftChatMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.MaterialData;
import org.bukkit.material.NetherWarts;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

public final class NMSLib_V1_12_R1 implements NMSHandler {

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(json)));
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
        PacketPlayOutTitle packetSubtitle;
        if (title != null) {
            packetSubtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, CraftChatMessage.fromString(title)[0], fadeIn, stay, fadeIn);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetSubtitle);
        }

        if (subtitle != null) {
            packetSubtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, CraftChatMessage.fromString(subtitle)[0], fadeIn, stay, fadeIn);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetSubtitle);
        }
    }

    @Override
    public void clearTitle(Player player) {
        PacketPlayOutTitle packetReset = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.RESET, null);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetReset);
    }


    @Override
    public ItemStack getItemInMainHand(Player player) {
        PlayerInventory inventory;
        return player == null ? null : ((inventory = player.getInventory()) == null ? null : inventory.getItemInMainHand());
    }


    @Override
    public Enchantment lookupEnchantment(String name, int internalId) {
        for (Enchantment value : Enchantment.values()) {
            String enchantmentName;
            if (value == null || (enchantmentName = value.getName()) == null) continue;
            if (enchantmentName.equals(name) || value.getId() == internalId) {
                return value;
            }
        }
        return null;
    }

    @Override
    public org.bukkit.enchantments.Enchantment buildEnchantment(EnchantInfo enchantInfo, Plugin plugin) {
        return new org.bukkit.enchantments.Enchantment(enchantInfo.getInternalId()) {
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
            public boolean conflictsWith(Enchantment enchantment) {
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
        for (Enchantment value : Enchantment.values()) {
            String enchantmentName;
            if (value == null || (enchantmentName = value.getName()) == null) continue;
            if (enchantmentName.equals(name) || value.getId() == internalId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean registerEnchantment(Enchantment enchantment) {
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
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
    public boolean isRegistered(Enchantment enchantment) {
        for (Enchantment value : Enchantment.values()) {
            if (value.equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isGrown(Block block, BlockState blockState) {
        if (block == null) {
            return true;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        if (blockState == null) {
            return true;
        }

        MaterialData data = blockState.getData();
        if (data instanceof Crops) {
            return ((Crops) data).getState() == CropState.RIPE;
        } else if (data instanceof NetherWarts) {
            return ((NetherWarts) data).getState() == NetherWartsState.RIPE;
        } else if (data instanceof CocoaPlant) {
            return ((CocoaPlant) data).getSize() == CocoaPlant.CocoaPlantSize.LARGE;
        } else if (data != null) {
            return data.getData() == (byte) 7;
        }

        return false;
    }

    @Override
    public void setCropToAdult(Block block, BlockState blockState) {
        if (block == null) {
            return ;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        if (blockState == null) {
            return ;
        }

        MaterialData data = blockState.getData();
        if (data instanceof NetherWarts) {
            NetherWarts netherWarts = (NetherWarts) data;
            if (netherWarts.getState() == NetherWartsState.RIPE) {
                return;
            }
            netherWarts.setState(NetherWartsState.RIPE);
            blockState.setData(netherWarts);
            blockState.update(true);
        } else if (data instanceof Crops) {
            Crops crops = (Crops) data;
            if (crops.getState() == CropState.RIPE) {
                return;
            }
            crops.setState(CropState.RIPE);
            blockState.setData(crops);
            blockState.update(true);
        } else if (data instanceof CocoaPlant) {
            CocoaPlant cocoaPlant = (CocoaPlant) data;
            cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.LARGE);

            // loop through the blocks around the cocoa plant
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block relative = block.getRelative(face);
                Material relativeType;
                if (relative != null &&((relativeType = relative.getType()) == Material.LOG || relativeType == Material.LOG_2)) {
                    cocoaPlant.setFacingDirection(face);
                    break;
                }
            }

            blockState.setData(cocoaPlant);
            blockState.update(true);
        } else {
            try {
                blockState.setRawData((byte) 7);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void setCropToBaby(Block block, BlockState blockState) {
        if (block == null) {
            return ;
        }

        if (blockState == null) {
            blockState = block.getState();
        }

        if (blockState == null) {
            return ;
        }

        MaterialData data = blockState.getData();
        if (data instanceof NetherWarts) {
            NetherWarts netherWarts = (NetherWarts) data;
            netherWarts.setState(NetherWartsState.SEEDED);
            blockState.setData(netherWarts);
            blockState.update(true);
        } else if (data instanceof Crops) {
            Crops crops = (Crops) data;
            crops.setState(CropState.SEEDED);
            blockState.setData(crops);
            blockState.update(true);
        } else if (data instanceof CocoaPlant) {
            CocoaPlant cocoaPlant = (CocoaPlant) data;
            cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.SMALL);

            // loop through the blocks around the cocoa plant
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block relative = block.getRelative(face);
                Material relativeType;
                if (relative != null && ((relativeType = relative.getType()) == Material.LOG || relativeType == Material.LOG_2)) {
                    cocoaPlant.setFacingDirection(face);
                    break;
                }
            }

            blockState.setData(cocoaPlant);
            blockState.update(true);
        } else {
            try {
                block.setData((byte) 7, false);
            } catch (Exception ignored) {
            }
        }
    }

}
