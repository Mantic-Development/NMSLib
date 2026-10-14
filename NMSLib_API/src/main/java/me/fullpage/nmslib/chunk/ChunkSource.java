package me.fullpage.nmslib.chunk;

public interface ChunkSource {

    /** Fills states[4096] (index {@code (y & 15) << 8 | (z & 15) << 4 | (x & 15)}) with block state ids for absolute section sectionY (blockY >> 4); returns false when the section is all air (states may then be left untouched). */
    boolean section(int sectionY, int[] states);
}
