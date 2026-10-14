package me.fullpage.nmslib.v26_2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import me.fullpage.nmslib.packet.DigAction;
import me.fullpage.nmslib.packet.DigListener;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

final class PacketInjector {

    private static final String HANDLER_NAME = "manticlib_packets";
    private static final Key KEY = Key.key("manticlib", "packets");
    private static final Entry[] NONE = new Entry[0];

    private static volatile Entry[] entries = NONE;
    private static boolean installed; // server thread only

    private record Entry(Plugin owner, DigListener listener) {
    }

    private PacketInjector() {
    }

    static void add(Plugin owner, DigListener listener) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(listener, "listener");
        synchronized (PacketInjector.class) {
            Entry[] current = entries;
            Entry[] next = Arrays.copyOf(current, current.length + 1);
            next[current.length] = new Entry(owner, listener);
            entries = next;
        }
        sync(owner);
    }

    static void remove(Plugin owner) {
        synchronized (PacketInjector.class) {
            Entry[] current = entries;
            int kept = 0;
            Entry[] next = new Entry[current.length];
            for (Entry entry : current) {
                if (entry.owner != owner) next[kept++] = entry;
            }
            if (kept == current.length) return;
            entries = kept == 0 ? NONE : Arrays.copyOf(next, kept);
        }
        sync(owner);
    }

    // Paper's listener map is a plain LinkedHashMap, so install/uninstall only on the server thread.
    private static void sync(Plugin owner) {
        if (!Bukkit.isPrimaryThread()) {
            MinecraftServer.getServer().execute(() -> sync(owner));
            return;
        }
        boolean wanted = entries.length != 0;
        if (wanted == installed) return;
        installed = wanted;
        if (!wanted) {
            ChannelInitializeListenerHolder.removeListener(KEY);
            return;
        }
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
        if (pipeline.get(HANDLER_NAME) != null || !(pipeline.get(HandlerNames.PACKET_HANDLER) instanceof Connection connection)) return;
        pipeline.addBefore(HandlerNames.PACKET_HANDLER, HANDLER_NAME, new Handler(connection));
    }

    private static final class Handler extends ChannelInboundHandlerAdapter {

        private final Connection connection;

        Handler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Entry[] current = entries;
            if (current.length == 0 || msg.getClass() != ServerboundPlayerActionPacket.class
                    || !dispatch(current, (ServerboundPlayerActionPacket) msg)) {
                ctx.fireChannelRead(msg);
            }
        }

        private boolean dispatch(Entry[] current, ServerboundPlayerActionPacket packet) {
            DigAction action = switch (packet.getAction()) {
                case START_DESTROY_BLOCK -> DigAction.START;
                case ABORT_DESTROY_BLOCK -> DigAction.ABORT;
                case STOP_DESTROY_BLOCK -> DigAction.STOP;
                default -> null;
            };
            if (action == null || !(connection.getPacketListener() instanceof ServerGamePacketListenerImpl listener)) return false;
            Player player = listener.player.getBukkitEntity();
            BlockPos pos = packet.getPos();
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            int face = packet.getDirection().get3DDataValue(), sequence = packet.getSequence();
            boolean drop = false;
            for (Entry entry : current) {
                try {
                    drop |= entry.listener.onDig(player, action, x, y, z, face, sequence);
                } catch (Throwable t) {
                    entry.owner.getLogger().log(Level.SEVERE, "Dig listener failed", t);
                }
            }
            return drop;
        }
    }
}
