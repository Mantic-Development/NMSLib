package me.fullpage.nmslib.v26_2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.fullpage.nmslib.chunk.ChunkSource;
import me.fullpage.nmslib.chunk.VirtualChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

final class VirtualChunks {

    private static final int AIR_ID = Block.getId(Blocks.AIR.defaultBlockState());
    private static final EnumSet<Heightmap.Types> CLIENT_HEIGHTMAPS = EnumSet.noneOf(Heightmap.Types.class);
    // Shared and never mutated: the light packet only calls isEmpty() and copy() on supplied layers.
    private static final DataLayer EMPTY_LIGHT = new DataLayer();
    private static final DataLayer FULL_LIGHT = new DataLayer(15);
    private static final LightChunkGetter NO_CHUNKS = new LightChunkGetter() {
        @Override
        public LightChunk getChunkForLighting(int x, int z) {
            return null;
        }

        @Override
        public BlockGetter getLevel() {
            return EmptyBlockGetter.INSTANCE;
        }
    };

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
        // Biomes, and light unless the source supplies it, come from the server's chunk.
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
        int[] skipped = new int[1];
        source.blockEntities((x, y, z, snbt) -> {
            if (!addBlockEntity(level, chunk, x, y, z, snbt)) skipped[0]++;
        });
        if (skipped[0] > 0) {
            Bukkit.getLogger().warning("[NMSLib] Skipped " + skipped[0] + " invalid block entities in virtual chunk " + chunkX + ", " + chunkZ);
        }
        // modifyBlocks=false skips anti-xray, which marks the packet ready in the constructor.
        return new Prepared(chunkX, chunkZ, level.dimension(),
                new ClientboundLevelChunkWithLightPacket(chunk, light(level, source), null, null, false));
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

    // The chunk is detached: setBlockEntity only stores the entity and sets its level field, with no tickers, listeners or level changes.
    private static boolean addBlockEntity(ServerLevel level, LevelChunk chunk, int x, int y, int z, String snbt) {
        if (snbt == null || x >> 4 != chunk.getPos().x() || z >> 4 != chunk.getPos().z() || level.isOutsideBuildHeight(y)) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = chunk.getBlockState(pos);
        if (!(state.getBlock() instanceof EntityBlock block)) return false;
        try {
            CompoundTag tag = TagParser.parseCompoundFully(snbt);
            BlockEntity blockEntity = block.newBlockEntity(pos, state);
            if (blockEntity == null) return false;
            String id = tag.getStringOr("id", null);
            if (id != null && !Objects.equals(Identifier.tryParse(id), BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()))) return false;
            blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            chunk.setBlockEntity(blockEntity);
            return true;
        } catch (CommandSyntaxException | RuntimeException e) {
            return false;
        }
    }

    private static LevelLightEngine light(ServerLevel level, ChunkSource source) {
        LevelLightEngine server = level.getLightEngine();
        int minSection = server.getMinLightSection();
        DataLayer[] sky = new DataLayer[server.getLightSectionCount()], block = new DataLayer[sky.length];
        byte[] skyScratch = new byte[DataLayer.SIZE], blockScratch = new byte[DataLayer.SIZE];
        boolean hasSky = level.dimensionType().hasSkyLight(), supplied = false;
        for (int i = 0; i < sky.length; i++) {
            Arrays.fill(skyScratch, (byte) 0);
            Arrays.fill(blockScratch, (byte) 0);
            boolean filled = source.light(minSection + i, skyScratch, blockScratch);
            supplied |= filled;
            // Null keeps sky out of both masks, as the server does without skylight.
            sky[i] = !hasSky ? null : filled ? layer(skyScratch) : EMPTY_LIGHT;
            block[i] = filled ? layer(blockScratch) : EMPTY_LIGHT;
        }
        return supplied ? new SuppliedLight(new Layer(minSection, sky), new Layer(minSection, block)) : server;
    }

    private static DataLayer layer(byte[] nibbles) {
        byte first = nibbles[0];
        if (first == 0 || first == -1) {
            int i = 1;
            while (i < nibbles.length && nibbles[i] == first) i++;
            if (i == nibbles.length) return first == 0 ? EMPTY_LIGHT : FULL_LIGHT;
        }
        return new DataLayer(nibbles.clone());
    }

    // Feeds supplied light through the vanilla light data constructor, so masks and wire format stay vanilla.
    private static final class SuppliedLight extends LevelLightEngine {
        private final Layer sky;
        private final Layer block;

        SuppliedLight(Layer sky, Layer block) {
            super(NO_CHUNKS, false, false); // no Level, so Starlight builds no engines and no server queue
            this.sky = sky;
            this.block = block;
        }

        @Override
        public LayerLightEventListener getLayerListener(LightLayer layer) {
            return layer == LightLayer.SKY ? sky : block;
        }

        @Override
        public int getLightSectionCount() {
            return sky.sections.length;
        }

        @Override
        public int getMinLightSection() {
            return sky.minSection;
        }
    }

    private record Layer(int minSection, DataLayer[] sections) implements LayerLightEventListener {
        @Override
        public DataLayer getDataLayerData(SectionPos pos) {
            return sections[pos.getY() - minSection];
        }

        @Override
        public int getLightValue(BlockPos pos) {
            return 0;
        }

        @Override
        public void checkBlock(BlockPos pos) {
        }

        @Override
        public boolean hasLightWork() {
            return false;
        }

        @Override
        public int runLightUpdates() {
            return 0;
        }

        @Override
        public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {
        }

        @Override
        public void setLightEnabled(ChunkPos pos, boolean enable) {
        }

        @Override
        public void propagateLightSources(ChunkPos pos) {
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
