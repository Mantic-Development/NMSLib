package me.fullpage.nmslib.v26_2;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import me.fullpage.nmslib.BlockPositions;
import me.fullpage.nmslib.NMSHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class BlockChanges {

    private static final long SECTION_MASK = ~(15L << 38 | 15L << 12 | 15L);
    // Half the client's 4096: a packet-level anticheat (e.g. CultAC) may add a ping after every sub-packet.
    static final int BUNDLE_LIMIT = 2047;

    private BlockChanges() {
    }

    static void send(Player viewer, long[] positions, int[] stateIds, int count) {
        NMSHandler.checkBlockChanges(positions, stateIds, count);
        ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;
        if (connection == null || count == 0) return;
        if (count == 1) {
            connection.send(new ClientboundBlockUpdatePacket(BlockPos.of(positions[0]), NMSLib_V26_2.state(stateIds[0])));
            return;
        }

        Long2IntOpenHashMap groups = new Long2IntOpenHashMap();
        groups.defaultReturnValue(-1);
        int[] groupOf = new int[count], sizes = new int[count], first = new int[count];
        int groupCount = 0;
        for (int i = 0; i < count; i++) {
            int group = groups.putIfAbsent(positions[i] & SECTION_MASK, groupCount);
            if (group < 0) {
                group = groupCount++;
                first[group] = i;
            }
            groupOf[i] = group;
            sizes[group]++;
        }

        short[][] relative = new short[groupCount][];
        BlockState[][] states = new BlockState[groupCount][];
        for (int group = 0; group < groupCount; group++) {
            relative[group] = new short[sizes[group]];
            states[group] = new BlockState[sizes[group]];
            sizes[group] = 0;
        }
        for (int i = 0; i < count; i++) {
            int group = groupOf[i], slot = sizes[group]++;
            long position = positions[i];
            relative[group][slot] = (short) ((BlockPositions.x(position) & 15) << 8 | (BlockPositions.z(position) & 15) << 4 | BlockPositions.y(position) & 15);
            states[group][slot] = NMSLib_V26_2.state(stateIds[i]);
        }

        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(groupCount);
        for (int group = 0; group < groupCount; group++) {
            long position = positions[first[group]];
            if (sizes[group] == 1) {
                packets.add(new ClientboundBlockUpdatePacket(BlockPos.of(position), states[group][0]));
            } else {
                SectionPos section = SectionPos.of(BlockPositions.x(position) >> 4, BlockPositions.y(position) >> 4, BlockPositions.z(position) >> 4);
                packets.add(new ClientboundSectionBlocksUpdatePacket(section, new ExactShortSet(relative[group]), states[group]));
            }
        }
        if (packets.size() == 1) {
            connection.send(packets.get(0));
            return;
        }
        for (int from = 0; from < packets.size(); from += BUNDLE_LIMIT) {
            connection.send(new ClientboundBundlePacket(packets.subList(from, Math.min(packets.size(), from + BUNDLE_LIMIT))));
        }
    }

    // The CraftBukkit packet constructor calls toShortArray(); hand it the exact-size array instead of a copy.
    private static final class ExactShortSet extends ShortArraySet {

        ExactShortSet(short[] positions) {
            super(positions);
        }

        @Override
        public short[] toShortArray() {
            return a;
        }
    }
}
