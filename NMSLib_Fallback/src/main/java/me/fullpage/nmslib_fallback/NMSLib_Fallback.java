package me.fullpage.nmslib_fallback;

import me.fullpage.nmslib.NMSHandler;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;

public final class NMSLib_Fallback implements NMSHandler {

    @Override
    public void setBlock(Location location, Material material) {
        location.getBlock().setType(material);
    }

    @Override
    public void setBlock(Location location, Material material, boolean applyPhysics) {
        this.setBlock(location.getBlock(), material, applyPhysics);

    }

    @Override
    public void setBlock(Location location, Material material, int durability) {
        this.setBlock(location.getBlock(), material, durability);

    }

    @Override
    public void setBlock(Location location, Material material, int durability, boolean applyPhysics) {
        this.setBlock(location.getBlock(), material, durability, applyPhysics);
    }

    @Override
    public void setBlock(Block block, Material material) {
        block.setType(material);

    }

    @Override
    public void setBlock(Block block, Material material, boolean applyPhysics) {
        block.setType(material, applyPhysics);
    }

    @Override
    public void setBlock(Block block, Material material, int durability) {
        block.setType(material);
        block.setData((byte) durability, false);
    }

    @Override
    public void setBlock(Block block, Material material, int durability, boolean applyPhysics) {
        block.setType(material, applyPhysics);
        block.setData((byte) durability, false);

    }

    @Override
    public void setBlock(World world, int x, int y, int z, Material material, boolean applyPhysics) {
        world.getBlockAt(x, y, z).setType(material, applyPhysics);

    }

    private static final String NMS_VERSION;
    private static final int VER;
    private static final MethodHandle PLAYER_CONNECTION;
    private static final MethodHandle GET_HANDLE;

    public static final Class<?> CRAFT_PLAYER;
    private static final String CRAFTBUKKIT, NMS;

    static {
        NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        VER = Integer.parseInt(NMS_VERSION.substring(1).split("_")[1]);
        CRAFTBUKKIT = "org.bukkit.craftbukkit." + NMS_VERSION + '.';
        NMS = 17 >= VER ? "net.minecraft." : "net.minecraft.server." + NMS_VERSION + '.';
        Class<?> entityPlayer = getNMSClass("server.level", "EntityPlayer");
        CRAFT_PLAYER = getCraftClass("entity.CraftPlayer");
        Class<?> playerConnection = getNMSClass("server.network", "PlayerConnection");

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle sendPacket = null;
        MethodHandle getHandle = null;
        MethodHandle connection = null;
        try {
            connection = lookup.findGetter(entityPlayer, 17 >= VER ? "b" : "playerConnection", playerConnection);
            getHandle = lookup.findVirtual(CRAFT_PLAYER, "getHandle", MethodType.methodType(entityPlayer));
            sendPacket = lookup.findVirtual(playerConnection, "sendPacket", MethodType.methodType(void.class, getNMSClass("network.protocol", "Packet")));
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }

        PLAYER_CONNECTION = connection;
        GET_HANDLE = getHandle;
    }

    private static Class<?> getCraftClass(String name) {
        try {
            return Class.forName(CRAFTBUKKIT + name);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static Class<?> getNMSClass(String newPackage, String name) {
        if (17 >= VER) name = newPackage + '.' + name;
        return getNMSClass(name);
    }

    private static Class<?> getNMSClass(String name) {
        try {
            return Class.forName(NMS + name);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    private Object getConnection(Player player) {
        Objects.requireNonNull(player, "Cannot get connection of null player");
        try {
            Object handle = GET_HANDLE.invoke(player);
            return PLAYER_CONNECTION.invoke(handle);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Override
    public void sendActionBar(Player player, String message) {
        if (!player.isOnline()) {
            return; // Player may have logged out
        }

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
    public void sendJsonMessage(Player player, String json) {
        player.spigot().sendMessage(ComponentSerializer.parse(json));
    }
}
