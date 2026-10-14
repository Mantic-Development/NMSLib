package me.fullpage.nmslib.v26_2;

import me.fullpage.nmslib.chunk.ChunkSource;
import me.fullpage.nmslib.chunk.VirtualChunk;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Objects;

final class VirtualChunks {

    private static final int AIR_ID = Block.getId(Blocks.AIR.defaultBlockState());
    private static final EnumSet<Heightmap.Types> CLIENT_HEIGHTMAPS = EnumSet.noneOf(Heightmap.Types.class);

    static {
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (type.sendToClient()) CLIENT_HEIGHTMAPS.add(type);
        }
    }

    private VirtualChunks() {
    }

    // The packet serializes blocks and copies light in its constructor, so one instance can go to any number of channels.
    private record Prepared(int chunkX, int chunkZ, ResourceKey<Level> dimension,
                            ClientboundLevelChunkWithLightPacket packet) implements VirtualChunk {
    }

    static VirtualChunk prepare(World world, int chunkX, int chunkZ, ChunkSource source) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(source, "source");
        checkThread();
        ServerLevel level = ((CraftWorld) world).getHandle();
        // Light and biomes come from the server's chunk, so only blocks differ from it.
        LevelChunk real = level.getChunkIfLoaded(chunkX, chunkZ);
        PalettedContainerFactory containers = PalettedContainerFactory.create(level.registryAccess());
        int minSectionY = level.getMinSectionY();
        LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];
        int[] scratch = new int[4096];
        for (int i = 0; i < sections.length; i++) {
            PalettedContainer<BlockState> states = containers.createForBlockStates();
            if (source.section(minSectionY + i, scratch)) fill(states, scratch);
            // Shared, never written: the section constructor and the packet only read biomes.
            PalettedContainer<Holder<Biome>> biomes = real != null
                    ? (PalettedContainer<Holder<Biome>>) real.getSection(i).getBiomes() : containers.createForBiomes();
            sections[i] = new LevelChunkSection(states, biomes); // counts blocks once
        }
        LevelChunk chunk = new LevelChunk(level, new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY,
                new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, sections, null, null);
        Heightmap.primeHeightmaps(chunk, CLIENT_HEIGHTMAPS);
        // modifyBlocks=false skips anti-xray, which marks the packet ready in the constructor.
        return new Prepared(chunkX, chunkZ, level.dimension(),
                new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null, false));
    }

    private static void fill(PalettedContainer<BlockState> states, int[] ids) {
        int lastId = AIR_ID;
        BlockState state = null;
        for (int i = 0; i < 4096; i++) {
            int id = ids[i];
            if (id == AIR_ID) continue;
            if (id != lastId) {
                state = NMSLib_V26_2.state(id);
                lastId = id;
            }
            states.getAndSetUnchecked(i & 15, i >>> 8, i >>> 4 & 15, state);
        }
    }

    static void send(Player viewer, VirtualChunk chunk) {
        Prepared prepared = (Prepared) Objects.requireNonNull(chunk, "chunk");
        checkThread();
        ServerPlayer player = ((CraftPlayer) viewer).getHandle();
        // A chunk from another dimension would land in the viewer's current world client-side.
        if (player.connection == null || player.level().dimension() != prepared.dimension) return;
        player.connection.send(prepared.packet);
    }

    private static void checkThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Virtual chunks must be used on the server thread");
    }
}
