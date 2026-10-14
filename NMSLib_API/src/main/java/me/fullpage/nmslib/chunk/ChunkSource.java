package me.fullpage.nmslib.chunk;

public interface ChunkSource {

    /**
     * Biome key such as {@code minecraft:plains} used for every section, or null to copy the loaded server chunk's biomes, which
     * limits prepareChunk to the server thread. Called once, first. An unknown key makes prepareChunk throw IllegalArgumentException.
     */
    default String biome() {
        return null;
    }

    /** Fills states[4096] (index {@code (y & 15) << 8 | (z & 15) << 4 | (x & 15)}) with block state ids for absolute section sectionY (blockY >> 4); returns false when the section is all air (states may then be left untouched). */
    boolean section(int sectionY, int[] states);

    /**
     * Fills the light of absolute light section sectionY, called after section and blockEntities for every sectionY from the
     * world's min section - 1 to its max section + 1 inclusive. Both arrays are 2048-byte vanilla nibble arrays (index
     * {@code y << 8 | z << 4 | x}, even index in the low nibble) zero-filled before each call; return true when they were filled.
     * If every call returns false the chunk keeps the server's light, which limits prepareChunk to the server thread; otherwise
     * sections returning false are sent as 0 sky and 0 block light. All-15 arrays (every byte 0xFF, e.g. open sky) and all-0 arrays
     * are detected and sent without a copy; sky light is ignored in dimensions without skylight.
     */
    default boolean light(int sectionY, byte[] skyLight, byte[] blockLight) {
        return false;
    }

    /**
     * Reports block entities, called once after section. Coordinates are world coordinates inside the chunk and snbt is the
     * vanilla block entity compound (id, x, y, z optional). The block state at the position decides the type: entries on states
     * without a block entity, with a different id, outside the chunk, at an already reported position or with data that cannot be
     * parsed or sent are skipped.
     */
    default void blockEntities(BlockEntitySink sink) {
    }
}
