package me.fullpage.nmslib.v1_8_r3;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import me.fullpage.nmslib.packet.BlockUpdateFilter;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange.MultiBlockChangeInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

final class BlockUpdateInjector implements Listener, AutoCloseable {
    private static final Field POSITION = field(PacketPlayOutBlockChange.class, "a");
    private static final Field CHUNK = field(PacketPlayOutMultiBlockChange.class, "a");
    private static final Field CHANGES = field(PacketPlayOutMultiBlockChange.class, "b");

    private final Plugin owner;
    private final BlockUpdateFilter filter;
    private final Map<UUID, Channel> channels = new HashMap<>();
    private final String name;

    BlockUpdateInjector(Plugin owner, BlockUpdateFilter filter) {
        this.owner = owner;
        this.filter = filter;
        name = owner.getName() + "_block_updates_" + System.identityHashCode(this);
        Bukkit.getPluginManager().registerEvents(this, owner);
        for (Player player : Bukkit.getOnlinePlayers()) inject(player);
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Missing 1.8 block packet field " + name, error);
        }
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
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        Channel channel = handle.playerConnection.networkManager.channel;
        if (channel == null) return;
        UUID id = player.getUniqueId();
        remove(channels.put(id, channel));
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get("packet_handler") != null) {
                channel.pipeline().addBefore("packet_handler", name, new Handler(handle, id));
            }
        });
    }

    private void remove(Channel channel) {
        if (channel == null) return;
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(name) != null) channel.pipeline().remove(name);
        });
    }

    private final class Handler extends ChannelOutboundHandlerAdapter {
        private final EntityPlayer player;
        private final UUID id;
        private boolean logged;

        Handler(EntityPlayer player, UUID id) {
            this.player = player;
            this.id = id;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
            Object outgoing = message;
            try {
                if (message instanceof PacketPlayOutBlockChange) {
                    PacketPlayOutBlockChange packet = (PacketPlayOutBlockChange) message;
                    BlockPosition pos = (BlockPosition) POSITION.get(packet);
                    IBlockData replacement = replace(player.world.getWorld().getUID(), pos, packet.block);
                    if (replacement != packet.block) {
                        PacketPlayOutBlockChange copy = new PacketPlayOutBlockChange();
                        POSITION.set(copy, pos);
                        copy.block = replacement;
                        outgoing = copy;
                    }
                } else if (message instanceof PacketPlayOutMultiBlockChange) {
                    outgoing = rewrite((PacketPlayOutMultiBlockChange) message, player.world.getWorld().getUID());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                if (!logged) {
                    logged = true;
                    owner.getLogger().log(Level.WARNING, "Could not filter a 1.8 block update", error);
                }
            }
            ctx.write(outgoing, promise);
        }

        private PacketPlayOutMultiBlockChange rewrite(PacketPlayOutMultiBlockChange packet, UUID world)
                throws ReflectiveOperationException {
            MultiBlockChangeInfo[] original = (MultiBlockChangeInfo[]) CHANGES.get(packet);
            PacketPlayOutMultiBlockChange copy = null;
            MultiBlockChangeInfo[] changed = null;
            for (int i = 0; i < original.length; i++) {
                MultiBlockChangeInfo entry = original[i];
                IBlockData replacement = replace(world, entry.a(), entry.c());
                if (replacement == entry.c()) continue;
                if (copy == null) {
                    copy = new PacketPlayOutMultiBlockChange();
                    CHUNK.set(copy, CHUNK.get(packet));
                    changed = original.clone();
                    CHANGES.set(copy, changed);
                }
                changed[i] = copy.new MultiBlockChangeInfo(entry.b(), replacement);
            }
            return copy == null ? packet : copy;
        }

        private IBlockData replace(UUID world, BlockPosition pos, IBlockData data) {
            int original = Block.getCombinedId(data);
            int replacement = filter.replace(id, world, pos.getX(), pos.getY(), pos.getZ(), original);
            return replacement < 0 || replacement == original ? data : Block.getByCombinedId(replacement);
        }
    }
}
