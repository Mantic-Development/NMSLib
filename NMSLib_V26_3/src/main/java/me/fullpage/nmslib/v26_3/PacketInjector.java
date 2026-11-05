package me.fullpage.nmslib.v26_3;

import io.netty.channel.*;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import me.fullpage.nmslib.packet.BlockUpdateFilter;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

final class PacketInjector {
    private static final String HANDLER_NAME = "manticlib_block_updates";
    private static final Key KEY = Key.key("manticlib", "block_updates");
    private static volatile Entry[] filters = new Entry[0];
    private static boolean installed;

    private PacketInjector() {
    }

    static AutoCloseable add(Plugin owner, BlockUpdateFilter listener) {
        Entry added = new Entry(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(listener, "listener"));
        synchronized (PacketInjector.class) {
            Entry[] next = Arrays.copyOf(filters, filters.length + 1);
            next[next.length - 1] = added;
            filters = next;
        }
        install(owner);
        return () -> remove(entry -> entry == added);
    }

    static void remove(Plugin owner) {
        remove(entry -> entry.owner == owner);
    }

    private static synchronized void remove(Predicate<Entry> match) {
        filters = Arrays.stream(filters).filter(entry -> !match.test(entry)).toArray(Entry[]::new);
    }

    // Installed once, never removed: Paper iterates its plain LinkedHashMap on netty threads; empty entries make the handler a pass-through.
    private static void install(Plugin owner) {
        if (!Bukkit.isPrimaryThread()) {
            MinecraftServer.getServer().execute(() -> install(owner));
            return;
        }
        if (installed) return;
        installed = true;
        ChannelInitializeListenerHolder.addListener(KEY, PacketInjector::inject);
        injectExisting();
        // Connections accepted before addListener wait in Paper's private pending queue until the next connection tick.
        if (owner.isEnabled()) Bukkit.getScheduler().runTaskLater(owner, PacketInjector::injectExisting, 3L);
    }

    private static void injectExisting() {
        List<Connection> connections = MinecraftServer.getServer().getConnection().getConnections();
        synchronized (connections) {
            for (Connection connection : connections) {
                Channel channel = connection.channel;
                if (channel != null && channel.isOpen()) channel.eventLoop().execute(() -> inject(channel));
            }
        }
    }

    private static void inject(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        ChannelHandler existing = pipeline.get(HANDLER_NAME);
        if (existing instanceof Handler || !(pipeline.get(HandlerNames.PACKET_HANDLER) instanceof Connection connection))
            return;
        // A handler left by a previous classloader (plugin reload) is replaced so it stops pinning that loader.
        if (existing != null) pipeline.replace(HANDLER_NAME, HANDLER_NAME, new Handler(connection));
        else pipeline.addBefore(HandlerNames.PACKET_HANDLER, HANDLER_NAME, new Handler(connection));
    }

    private record Entry(Plugin owner, BlockUpdateFilter listener) {
    }

    private static final class Handler extends ChannelOutboundHandlerAdapter {
        private final Connection connection;
        private boolean filterFailureLogged;

        Handler(Connection connection) {
            this.connection = connection;
        }

        private static Object rewriteSection(ClientboundSectionBlocksUpdatePacket packet, Entry[] current, UUID player, UUID world) {
            Short2ObjectLinkedOpenHashMap<BlockState> replacements = new Short2ObjectLinkedOpenHashMap<>();
            SectionPos[] section = new SectionPos[1];
            packet.runUpdates((pos, state) -> {
                BlockState replacement = replace(current, player, world, pos, state);
                if (replacement != state) {
                    if (section[0] == null) section[0] = SectionPos.of(pos);
                    replacements.put(SectionPos.sectionRelativePos(pos), replacement);
                }
            });
            if (section[0] == null) return packet;

            Short2ObjectLinkedOpenHashMap<BlockState> states = new Short2ObjectLinkedOpenHashMap<>();
            packet.runUpdates((pos, state) -> {
                short relative = SectionPos.sectionRelativePos(pos);
                states.put(relative, replacements.getOrDefault(relative, state));
            });
            return new ClientboundSectionBlocksUpdatePacket(section[0], states);
        }

        private static BlockState replace(Entry[] current, UUID player, UUID world, BlockPos pos, BlockState state) {
            int original = Block.getId(state);
            for (Entry entry : current) {
                int id = entry.listener.replace(player, world, pos.getX(), pos.getY(), pos.getZ(), original);
                if (id >= 0 && id != original) return Block.stateById(id);
            }
            return state;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            Entry[] current = filters;
            Object outgoing = msg;
            if (current.length != 0 && (msg instanceof ClientboundBlockUpdatePacket
                    || msg instanceof ClientboundSectionBlocksUpdatePacket)
                    && connection.getPacketListener() instanceof ServerGamePacketListenerImpl listener) {
                try {
                    UUID player = listener.player.getUUID();
                    UUID world = listener.player.level().getWorld().getUID();
                    if (msg instanceof ClientboundBlockUpdatePacket packet) {
                        BlockState state = replace(current, player, world, packet.getPos(), packet.getBlockState());
                        if (state != packet.getBlockState())
                            outgoing = new ClientboundBlockUpdatePacket(packet.getPos(), state);
                    } else {
                        outgoing = rewriteSection((ClientboundSectionBlocksUpdatePacket) msg, current, player, world);
                    }
                } catch (RuntimeException | LinkageError error) {
                    if (!filterFailureLogged) {
                        filterFailureLogged = true;
                        current[0].owner.getLogger().log(Level.WARNING, "Could not filter an outbound block update", error);
                    }
                }
            }
            ctx.write(outgoing, promise);
        }
    }
}
