package me.fullpage.nmslib.v26_2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.papermc.paper.util.IntervalledCounter;
import me.fullpage.nmslib.packet.DigAction;
import me.fullpage.nmslib.packet.DigListener;
import me.fullpage.nmslib.packet.UseItemOnListener;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;

final class PacketInjector {

    private static final String HANDLER_NAME = "manticlib_packets";
    private static final Key KEY = Key.key("manticlib", "packets");
    private static final Entry[] NONE = new Entry[0];

    private static volatile Entry[] digs = NONE;
    private static volatile Entry[] uses = NONE;
    private static boolean installed; // server thread only

    private record Entry(Plugin owner, Object listener) {
    }

    private PacketInjector() {
    }

    static AutoCloseable add(Plugin owner, DigListener listener) {
        return add(owner, listener, true);
    }

    static AutoCloseable add(Plugin owner, UseItemOnListener listener) {
        return add(owner, listener, false);
    }

    private static AutoCloseable add(Plugin owner, Object listener, boolean dig) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(listener, "listener");
        Entry added = new Entry(owner, listener);
        synchronized (PacketInjector.class) {
            if (dig) digs = with(digs, added);
            else uses = with(uses, added);
        }
        install(owner);
        return () -> remove(entry -> entry == added);
    }

    static void remove(Plugin owner) {
        remove(entry -> entry.owner == owner);
    }

    private static void remove(Predicate<Entry> match) {
        synchronized (PacketInjector.class) {
            digs = without(digs, match);
            uses = without(uses, match);
        }
    }

    private static Entry[] with(Entry[] current, Entry added) {
        Entry[] next = Arrays.copyOf(current, current.length + 1);
        next[current.length] = added;
        return next;
    }

    private static Entry[] without(Entry[] current, Predicate<Entry> match) {
        int kept = 0;
        Entry[] next = new Entry[current.length];
        for (Entry entry : current) {
            if (!match.test(entry)) next[kept++] = entry;
        }
        return kept == current.length ? current : kept == 0 ? NONE : Arrays.copyOf(next, kept);
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
        if (existing instanceof Handler || !(pipeline.get(HandlerNames.PACKET_HANDLER) instanceof Connection connection)) return;
        // A handler left by a previous classloader (plugin reload) is replaced so it stops pinning that loader.
        if (existing != null) pipeline.replace(HANDLER_NAME, HANDLER_NAME, new Handler(connection));
        else pipeline.addBefore(HandlerNames.PACKET_HANDLER, HANDLER_NAME, new Handler(connection));
    }

    private static final class Handler extends ChannelInboundHandlerAdapter {

        private final Connection connection;
        private IntervalledCounter dropped;
        private boolean flooding;

        Handler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Class<?> type = msg.getClass();
            boolean drop = !flooding && (type == ServerboundPlayerActionPacket.class ? dig((ServerboundPlayerActionPacket) msg)
                    : type == ServerboundUseItemOnPacket.class && useItemOn((ServerboundUseItemOnPacket) msg));
            if (!drop || !countDrop()) ctx.fireChannelRead(msg);
        }

        // Dropped packets never reach Paper's limiter, so count them here; past the limit everything is forwarded and Paper kicks.
        private boolean countDrop() {
            GlobalConfiguration.PacketLimiter.PacketLimit limit = GlobalConfiguration.get().packetLimiter.allPackets;
            if (!limit.isEnabled()) return true;
            if (dropped == null) dropped = new IntervalledCounter((long) (limit.interval() * 1.0e9));
            dropped.updateAndAdd(1, System.nanoTime());
            if (dropped.getRate() < limit.maxPacketRate()) return true;
            flooding = true;
            return false;
        }

        private boolean dig(ServerboundPlayerActionPacket packet) {
            Entry[] current = digs;
            if (current.length == 0) return false;
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
                    drop |= ((DigListener) entry.listener).onDig(player, action, x, y, z, face, sequence);
                } catch (Throwable t) {
                    entry.owner.getLogger().log(Level.SEVERE, "Dig listener failed", t);
                }
            }
            return drop;
        }

        private boolean useItemOn(ServerboundUseItemOnPacket packet) {
            Entry[] current = uses;
            if (current.length == 0 || !(connection.getPacketListener() instanceof ServerGamePacketListenerImpl listener)) return false;
            Player player = listener.player.getBukkitEntity();
            BlockHitResult hit = packet.getHitResult();
            BlockPos pos = hit.getBlockPos();
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            int face = hit.getDirection().get3DDataValue(), sequence = packet.getSequence();
            boolean mainHand = packet.getHand() == InteractionHand.MAIN_HAND;
            boolean drop = false;
            for (Entry entry : current) {
                try {
                    drop |= ((UseItemOnListener) entry.listener).onUseItemOn(player, x, y, z, face, sequence, mainHand);
                } catch (Throwable t) {
                    entry.owner.getLogger().log(Level.SEVERE, "Use-item-on listener failed", t);
                }
            }
            return drop;
        }
    }
}
