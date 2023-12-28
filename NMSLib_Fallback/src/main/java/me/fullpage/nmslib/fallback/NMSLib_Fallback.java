package me.fullpage.nmslib.fallback;

import me.fullpage.manticlib.utils.ReflectionUtils;
import me.fullpage.nmslib.EnchantInfo;
import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public final class NMSLib_Fallback implements NMSHandler {


    private static Object TITLE, SUBTITLE, TIMES, CLEAR;
    private static MethodHandle PACKET_PLAY_OUT_TITLE;
    private static MethodHandle CHAT_COMPONENT_TEXT;

    static {
        try {
            MethodHandle packetCtor = null;
            MethodHandle chatComp = null;

            Object times = null;
            Object title = null;
            Object subtitle = null;
            Object clear = null;

            if (!ReflectionUtils.supports(11)) {
                Class<?> chatComponentText = ReflectionUtils.getNMSClass("ChatComponentText");
                Class<?> packet = ReflectionUtils.getNMSClass("PacketPlayOutTitle");
                Class<?> titleTypes = packet.getDeclaredClasses()[0];

                for (Object type : titleTypes.getEnumConstants()) {
                    switch (type.toString()) {
                        case "TIMES":
                            times = type;
                            break;
                        case "TITLE":
                            title = type;
                            break;
                        case "SUBTITLE":
                            subtitle = type;
                            break;
                        case "CLEAR":
                            clear = type;
                    }
                }

                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    chatComp = lookup.findConstructor(chatComponentText, MethodType.methodType(void.class, String.class));

                    packetCtor = lookup.findConstructor(packet,
                            MethodType.methodType(void.class, titleTypes,
                                    ReflectionUtils.getNMSClass("IChatBaseComponent"), int.class, int.class, int.class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            TITLE = title;
            SUBTITLE = subtitle;
            TIMES = times;
            CLEAR = clear;

            PACKET_PLAY_OUT_TITLE = packetCtor;
            CHAT_COMPONENT_TEXT = chatComp;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private static Class<?> getCraftClass(String name) {
        return ReflectionUtils.getCraftClass(name);
    }

    private static Class<?> getNMSClass(String newPackage, String name) {
        return getNMSClass(newPackage, name);
    }

    private Object getConnection(Player player) {
        return ReflectionUtils.getConnection(player);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        if (!player.isOnline()) {
            return; // Player may have logged out
        }

        String NMS_VERSION = ReflectionUtils.VERSION;
        int VER = ReflectionUtils.VER;

        try {
            Object connection = this.getConnection(player);
            Object packet;
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".PacketPlayOutChat");
            Class<?> packetClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".Packet");
            if (VER == 7 || VER == 8 || VER == 9) {
                try {
                    Class<?> ppoc = Class.forName("net.minecraft.server." + NMS_VERSION + ".PacketPlayOutChat");
                    Object packetPlayOutChat;
                    Class<?> chat = Class.forName("net.minecraft.server." + NMS_VERSION + (NMS_VERSION.equalsIgnoreCase("v1_8_R1") ? ".ChatSerializer" : ".ChatComponentText"));
                    Class<?> chatBaseComponent = Class.forName("net.minecraft.server." + NMS_VERSION + ".IChatBaseComponent");

                    Method method = null;
                    if (NMS_VERSION.equalsIgnoreCase("v1_8_R1")) method = chat.getDeclaredMethod("a", String.class);

                    Object object = NMS_VERSION.equalsIgnoreCase("v1_8_R1") ? chatBaseComponent.cast(method.invoke(chat, "{'text': '" + message + "'}")) : chat.getConstructor(new Class[]{String.class}).newInstance(message);
                    packetPlayOutChat = ppoc.getConstructor(new Class[]{chatBaseComponent, Byte.TYPE}).newInstance(object, (byte) 2);

                    Method sendPacketMethod = connection.getClass().getDeclaredMethod("sendPacket", packetClass);
                    sendPacketMethod.invoke(connection, packetPlayOutChat);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            } else {
                Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".ChatComponentText");
                Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".IChatBaseComponent");
                try {
                    Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + NMS_VERSION + ".ChatMessageType");
                    Object[] chatMessageTypes = chatMessageTypeClass.getEnumConstants();
                    Object chatMessageType = null;
                    for (Object obj : chatMessageTypes) {
                        if (obj.toString().equals("GAME_INFO")) {
                            chatMessageType = obj;
                        }
                    }
                    Object chatCompontentText = chatComponentTextClass.getConstructor(new Class<?>[]{String.class}).newInstance(message);
                    packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, chatMessageTypeClass}).newInstance(chatCompontentText, chatMessageType);
                } catch (ClassNotFoundException cnfe) {
                    Object chatCompontentText = chatComponentTextClass.getConstructor(new Class<?>[]{String.class}).newInstance(message);
                    packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, byte.class}).newInstance(chatCompontentText, (byte) 2);
                }
            }
            Method sendPacketMethod = connection.getClass().getDeclaredMethod("sendPacket", packetClass);
            sendPacketMethod.invoke(connection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 20, 10);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "Cannot send title to null player");
        if (title == null && subtitle == null) return;
        if (ReflectionUtils.supports(11)) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            return;
        }

        try {
            Object timesPacket = PACKET_PLAY_OUT_TITLE.invoke(TIMES, CHAT_COMPONENT_TEXT.invoke(title), fadeIn, stay, fadeOut);
            ReflectionUtils.sendPacket(player, timesPacket);

            if (title != null) {
                Object titlePacket = PACKET_PLAY_OUT_TITLE.invoke(TITLE, CHAT_COMPONENT_TEXT.invoke(title), fadeIn, stay, fadeOut);
                ReflectionUtils.sendPacket(player, titlePacket);
            }
            if (subtitle != null) {
                Object subtitlePacket = PACKET_PLAY_OUT_TITLE.invoke(SUBTITLE, CHAT_COMPONENT_TEXT.invoke(subtitle), fadeIn, stay, fadeOut);
                ReflectionUtils.sendPacket(player, subtitlePacket);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void clearTitle(Player player) {
        Objects.requireNonNull(player, "Cannot clear title from null player");
        if (ReflectionUtils.supports(11)) {
            player.resetTitle();
            return;
        }

        Object clearPacket;
        try {
            clearPacket = PACKET_PLAY_OUT_TITLE.invoke(CLEAR, null, -1, -1, -1);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return;
        }

        ReflectionUtils.sendPacket(player, clearPacket);
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        player.spigot().sendMessage(ComponentSerializer.parse(json));
    }

    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        if (ReflectionUtils.supports(9)) {
            return event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;
        }
        return true;
    }

    @Override
    public ItemStack getItemInMainHand(Player player) {
        return player == null ? null : player.getItemInHand();
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
    public boolean registerEnchantment(Enchantment enchantment) {
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
            f.setAccessible(false);
            Enchantment.registerEnchantment(enchantment);
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


}
