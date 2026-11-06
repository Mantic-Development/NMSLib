package me.fullpage.nmslib.packet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Shared native block-update hook for the versioned CraftBukkit and Mojang-mapped 1.x servers.
 */
public final class IntermediateBlockUpdates implements Listener, AutoCloseable {
    private final Plugin owner;
    private final BlockUpdateFilter filter;
    private final Layout layout;
    private final Map<UUID, Channel> channels = new HashMap<>();
    private final Map<Object, Integer> stateIds = new ConcurrentHashMap<>();
    private final Map<Integer, Object> states = new ConcurrentHashMap<>();
    private final AtomicInteger nextState = new AtomicInteger();
    private final String name;

    public IntermediateBlockUpdates(Plugin owner, BlockUpdateFilter filter) throws ReflectiveOperationException {
        this.owner = owner;
        this.filter = filter;
        this.layout = new Layout(Bukkit.getServer().getClass().getPackage().getName(), Bukkit.getServer().getClass().getClassLoader());
        name = owner.getName() + "_native_block_updates_" + System.identityHashCode(this);
        try {
            Bukkit.getPluginManager().registerEvents(this, owner);
            for (Player player : Bukkit.getOnlinePlayers()) inject(player);
        } catch (RuntimeException | LinkageError error) {
            close();
            throw error;
        }
    }

    @SuppressWarnings("deprecation")
    public int stateId(Object[] data) throws ReflectiveOperationException {
        if (data.length == 1) return remember(layout.craftState.invoke(data[0]));
        Object block = layout.legacyBlock.invoke(null, ((Material) data[0]).getId());
        return block == null ? -1 : remember(layout.legacyState.invoke(block, (int) (byte) data[1]));
    }

    private int remember(Object state) {
        return stateIds.computeIfAbsent(state, value -> {
            int id = nextState.getAndIncrement();
            states.put(id, value);
            return id;
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(channels.remove(event.getPlayer().getUniqueId()));
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        channels.values().forEach(this::remove);
        channels.clear();
    }

    private void inject(Player player) {
        try {
            Object handle = layout.handle.invoke(player);
            Object connection = layout.playerConnection.get(handle);
            if (connection == null) return;
            Channel channel = (Channel) layout.channel.get(layout.network.get(connection));
            if (channel == null) return;
            remove(channels.put(player.getUniqueId(), channel));
            channel.eventLoop().execute(() -> {
                if (channel.pipeline().get("packet_handler") != null) {
                    channel.pipeline().addBefore("packet_handler", name, new Handler(player));
                }
            });
        } catch (ReflectiveOperationException | RuntimeException error) {
            owner.getLogger().log(Level.WARNING, "Could not attach native block-update filter", error);
        }
    }

    private void remove(Channel channel) {
        if (channel == null) return;
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(name) != null) channel.pipeline().remove(name);
        });
    }

    private static final class Layout {
        final Method handle, craftState, legacyBlock, legacyState;
        final Field playerConnection, network, channel, position, state, origin, offsets, entries, entryOffset, entryState, lightFlag;
        final Class<?> single, multi;
        final Constructor<?> singleConstructor, multiConstructor, entryConstructor;
        final Object emptyOffsets;
        final Coords blockCoords, multiCoords;

        Layout(String craft, ClassLoader loader) throws ReflectiveOperationException {
            Class<?> craftPlayer = Class.forName(craft + ".entity.CraftPlayer", false, loader);
            handle = craftPlayer.getMethod("getHandle");
            playerConnection = field(handle.getReturnType(), "PlayerConnection", "ServerGamePacketListenerImpl");
            network = field(playerConnection.getType(), "NetworkManager", "Connection");
            channel = field(network.getType(), "Channel");
            String version = craft.substring(craft.lastIndexOf('.') + 1);
            String old = "net.minecraft.server." + version + ".";
            String game = "net.minecraft.network.protocol.game.";
            single = load(loader, old + "PacketPlayOutBlockChange", game + "PacketPlayOutBlockChange", game + "ClientboundBlockUpdatePacket");
            multi = load(loader, old + "PacketPlayOutMultiBlockChange", game + "PacketPlayOutMultiBlockChange", game + "ClientboundSectionBlocksUpdatePacket");
            position = field(single, "BlockPosition", "BlockPos");
            state = field(single, "IBlockData", "BlockState");
            blockCoords = new Coords(position.getType(), false);
            Constructor<?> singleNative = constructor(single, position.getType(), state.getType());
            singleConstructor = singleNative == null ? single.getDeclaredConstructor() : singleNative;
            origin = field(multi, "ChunkCoordIntPair", "ChunkPos", "SectionPosition", "SectionPos");
            offsets = optionalField(multi, "short[]");
            multiCoords = new Coords(origin.getType(), offsets == null);
            lightFlag = optionalField(multi, "boolean");
            if (offsets == null) {
                entries = field(multi, "MultiBlockChangeInfo[]");
                Class<?> entry = entries.getType().getComponentType();
                entryOffset = field(entry, "short");
                entryState = field(entry, state.getType().getSimpleName());
                Constructor<?> inner = constructor(entry, multi, short.class, state.getType());
                entryConstructor = inner == null ? entry.getDeclaredConstructor(short.class, state.getType()) : inner;
                entryConstructor.setAccessible(true);
                multiConstructor = multi.getDeclaredConstructor();
                emptyOffsets = null;
            } else {
                entries = field(multi, state.getType().getSimpleName() + "[]");
                entryOffset = entryState = null;
                entryConstructor = null;
                Constructor<?> noArg = constructor(multi);
                multiConstructor = noArg == null ? Arrays.stream(multi.getConstructors())
                        .filter(c -> c.getParameterCount() >= 3 && c.getParameterTypes()[1].getSimpleName().equals("ShortSet"))
                        .findFirst().orElseThrow(() -> new NoSuchMethodException(multi.getName())) : noArg;
                if (multiConstructor.getParameterCount() == 0) emptyOffsets = null;
                else {
                    String pkg = multiConstructor.getParameterTypes()[1].getPackage().getName();
                    emptyOffsets = Class.forName(pkg + ".ShortArraySet", true, loader).getConstructor().newInstance();
                }
            }
            Method modern = null;
            try {
                modern = Class.forName(craft + ".block.data.CraftBlockData", false, loader).getMethod("getState");
            } catch (ClassNotFoundException ignored) {
                // Legacy Bukkit uses material ids and metadata.
            }
            craftState = modern;
            if (modern == null) {
                Class<?> block = Class.forName(old + "Block", false, loader);
                legacyBlock = block.getMethod("getById", int.class);
                legacyState = block.getMethod("fromLegacyData", int.class);
            } else {
                legacyBlock = legacyState = null;
            }
        }

        private static Class<?> load(ClassLoader loader, String... names) throws ClassNotFoundException {
            for (String name : names) {
                try {
                    return Class.forName(name, false, loader);
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw new ClassNotFoundException(Arrays.toString(names));
        }

        private static Constructor<?> constructor(Class<?> type, Class<?>... parameters) {
            try {
                return type.getConstructor(parameters);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }

        private static Field field(Class<?> type, String... names) throws NoSuchFieldException {
            Field field = optionalField(type, names);
            if (field == null) throw new NoSuchFieldException(type.getName() + " " + Arrays.toString(names));
            return field;
        }

        private static Field optionalField(Class<?> type, String... names) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && Arrays.asList(names).contains(field.getType().getSimpleName())) {
                        field.setAccessible(true);
                        return field;
                    }
                }
            }
            return null;
        }

        Object copyMulti(Object packet, Object section) throws ReflectiveOperationException {
            Object copy;
            int count = multiConstructor.getParameterCount();
            if (count == 0) copy = multiConstructor.newInstance();
            else {
                Class<?> dataType = multiConstructor.getParameterTypes()[2];
                Object empty = dataType.isArray() ? java.lang.reflect.Array.newInstance(dataType.getComponentType(), 0) : null;
                copy = count == 4 ? multiConstructor.newInstance(section, emptyOffsets, empty, lightFlag.getBoolean(packet))
                        : multiConstructor.newInstance(section, emptyOffsets, empty);
            }
            origin.set(copy, section);
            if (offsets != null) offsets.set(copy, offsets.get(packet));
            if (lightFlag != null) lightFlag.set(copy, lightFlag.get(packet));
            return copy;
        }
    }

    private static final class Coords {
        private final Method x, y, z;
        private final Field chunkX, chunkZ;

        Coords(Class<?> type, boolean chunk) throws ReflectiveOperationException {
            if (chunk) {
                chunkX = type.getField("x");
                chunkZ = type.getField("z");
                x = y = z = null;
            } else {
                x = type.getMethod("getX");
                y = type.getMethod("getY");
                z = type.getMethod("getZ");
                chunkX = chunkZ = null;
            }
        }

        int x(Object value) throws ReflectiveOperationException {
            return x == null ? chunkX.getInt(value) : (int) x.invoke(value);
        }

        int y(Object value) throws ReflectiveOperationException {
            return (int) y.invoke(value);
        }

        int z(Object value) throws ReflectiveOperationException {
            return z == null ? chunkZ.getInt(value) : (int) z.invoke(value);
        }
    }

    private final class Handler extends ChannelOutboundHandlerAdapter {
        private final Player player;
        private final UUID id;
        private boolean logged;

        Handler(Player player) {
            this.player = player;
            this.id = player.getUniqueId();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
            Object packet = message;
            try {
                if (layout.single.isInstance(message)) packet = single(message, player.getWorld().getUID());
                else if (layout.multi.isInstance(message)) packet = multi(message, player.getWorld().getUID());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                if (!logged) {
                    logged = true;
                    owner.getLogger().log(Level.WARNING, "Could not rewrite a native block update", error);
                }
            }
            ctx.write(packet, promise);
        }

        private Object single(Object packet, UUID world) throws ReflectiveOperationException {
            Object pos = layout.position.get(packet);
            Object original = layout.state.get(packet);
            Object replacement = replace(world, layout.blockCoords.x(pos), layout.blockCoords.y(pos), layout.blockCoords.z(pos), original);
            if (replacement == original) return packet;
            if (layout.singleConstructor.getParameterCount() != 0)
                return layout.singleConstructor.newInstance(pos, replacement);
            Object copy = layout.singleConstructor.newInstance();
            layout.position.set(copy, pos);
            layout.state.set(copy, replacement);
            return copy;
        }

        private Object multi(Object packet, UUID world) throws ReflectiveOperationException {
            Object origin = layout.origin.get(packet);
            int x = layout.multiCoords.x(origin) << 4;
            int z = layout.multiCoords.z(origin) << 4;
            int y = layout.offsets == null ? 0 : layout.multiCoords.y(origin) << 4;
            Object[] entries = (Object[]) layout.entries.get(packet);
            short[] offsets = layout.offsets == null ? null : (short[]) layout.offsets.get(packet);
            Object[] changed = null;
            Object copy = null;
            for (int i = 0; i < entries.length; i++) {
                Object entry = entries[i];
                int packed = offsets == null ? layout.entryOffset.getShort(entry) : offsets[i];
                int dx = (packed >>> (offsets == null ? 12 : 8)) & 15;
                int dz = (packed >>> (offsets == null ? 8 : 4)) & 15;
                int dy = packed & (offsets == null ? 255 : 15);
                Object original = offsets == null ? layout.entryState.get(entry) : entry;
                Object replacement = replace(world, x + dx, y + dy, z + dz, original);
                if (replacement == original) continue;
                if (copy == null) {
                    copy = layout.copyMulti(packet, origin);
                    changed = entries.clone();
                }
                changed[i] = offsets != null ? replacement : layout.entryConstructor.getParameterCount() == 3
                                                             ? layout.entryConstructor.newInstance(copy, (short) packed, replacement)
                                                             : layout.entryConstructor.newInstance((short) packed, replacement);
            }
            if (copy == null) return packet;
            layout.entries.set(copy, changed);
            return copy;
        }

        private Object replace(UUID world, int x, int y, int z, Object original) {
            Integer state = stateIds.get(original);
            if (state == null) return original;
            int replacement = filter.replace(id, world, x, y, z, state);
            return replacement < 0 ? original : states.getOrDefault(replacement, original);
        }
    }
}
