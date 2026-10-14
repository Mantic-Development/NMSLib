package me.fullpage.nmslib.v26_2;

import me.fullpage.nmslib.NMSHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

final class BlockFill {

    private BlockFill() {
    }

    static int fill(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                    int[] stateIds, int[] cumulativeWeights, long seed) {
        NMSHandler.checkFill(world, stateIds, cumulativeWeights);
        BlockState[] states = new BlockState[stateIds.length];
        for (int i = 0; i < states.length; i++) {
            states[i] = NMSLib_V26_2.state(stateIds[i]);
            if (states[i].hasBlockEntity()) throw new IllegalArgumentException("fillBlocks does not create block entities: " + states[i]);
        }
        ServerLevel level = ((CraftWorld) world).getHandle();
        minY = Math.max(minY, level.getMinY());
        maxY = Math.min(maxY, level.getMaxY());
        if (minX > maxX || minY > maxY || minZ > maxZ) return 0;

        Picker picker = new Picker(states, cumulativeWeights, seed);
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        List<ChunkPos> relight = new ArrayList<>();
        int changed = 0;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = level.getChunkIfLoaded(cx, cz);
                if (chunk == null) continue;
                int chunkChanged = fillChunk(chunk, lightEngine, picker, relight,
                        Math.max(minX, cx << 4), minY, Math.max(minZ, cz << 4),
                        Math.min(maxX, (cx << 4) + 15), maxY, Math.min(maxZ, (cz << 4) + 15));
                if (chunkChanged == 0) continue;
                changed += chunkChanged;
                chunk.markUnsaved();
                resend(level, chunk);
            }
        }
        if (!relight.isEmpty()) lightEngine.starlight$serverRelightChunks(relight, null, null);
        return changed;
    }

    private static int fillChunk(LevelChunk chunk, ThreadedLevelLightEngine lightEngine, Picker picker, List<ChunkPos> relight,
                                 int fromX, int minY, int fromZ, int toX, int maxY, int toZ) {
        Heightmap motionBlocking = chunk.heightmaps.get(Heightmap.Types.MOTION_BLOCKING);
        Heightmap motionBlockingNoLeaves = chunk.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        Heightmap oceanFloor = chunk.heightmaps.get(Heightmap.Types.OCEAN_FLOOR);
        Heightmap worldSurface = chunk.heightmaps.get(Heightmap.Types.WORLD_SURFACE);
        boolean blockEntities = !chunk.getBlockEntities().isEmpty();
        boolean lightChanged = false;
        LevelChunkSection[] sections = chunk.getSections();
        int changed = 0;
        for (int sectionY = maxY >> 4; sectionY >= minY >> 4; sectionY--) {
            LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(sectionY)];
            boolean wasEmpty = section.hasOnlyAir();
            if (wasEmpty && picker.single != null && picker.single.isAir()) continue;
            int bottom = Math.max(minY, sectionY << 4);
            for (int y = Math.min(maxY, (sectionY << 4) + 15); y >= bottom; y--) {
                int localY = y & 15;
                for (int z = fromZ; z <= toZ; z++) {
                    int localZ = z & 15;
                    for (int x = fromX; x <= toX; x++) {
                        int localX = x & 15;
                        BlockState state = picker.next();
                        BlockState old = section.setBlockState(localX, localY, localZ, state, false);
                        if (old == state) continue;
                        changed++;
                        motionBlocking.update(localX, y, localZ, state);
                        motionBlockingNoLeaves.update(localX, y, localZ, state);
                        oceanFloor.update(localX, y, localZ, state);
                        worldSurface.update(localX, y, localZ, state);
                        if (blockEntities && old.hasBlockEntity()) chunk.removeBlockEntity(new BlockPos(x, y, z));
                        if (!lightChanged) lightChanged = LightEngine.hasDifferentLightProperties(old, state);
                    }
                }
            }
            boolean isEmpty = section.hasOnlyAir();
            if (wasEmpty != isEmpty) lightEngine.updateSectionStatus(SectionPos.of(chunk.getPos(), sectionY), isEmpty);
        }
        if (lightChanged) relight.add(chunk.getPos());
        return changed;
    }

    private static void resend(ServerLevel level, LevelChunk chunk) {
        ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunk.getPos().pack());
        if (holder == null || !holder.moonrise$hasChunkBeenSent()) return;
        io.papermc.paper.FeatureHooks.sendChunkRefreshPackets(holder.moonrise$getPlayers(false), chunk);
    }

    private static final class Picker {

        final BlockState single;
        private final BlockState[] states;
        private final int[] cumulativeWeights;
        private final int total;
        private final SplittableRandom random;

        Picker(BlockState[] states, int[] cumulativeWeights, long seed) {
            this.single = states.length == 1 ? states[0] : null;
            this.states = states;
            this.cumulativeWeights = cumulativeWeights;
            this.total = cumulativeWeights[cumulativeWeights.length - 1];
            this.random = single == null ? new SplittableRandom(seed) : null;
        }

        BlockState next() {
            if (single != null) return single;
            int roll = random.nextInt(total), i = 0;
            while (roll >= cumulativeWeights[i]) i++;
            return states[i];
        }
    }
}
