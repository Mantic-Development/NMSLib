package me.fullpage.nmslib.chunk;

/** Immutable prepared chunk packet, reusable for any number of viewers and sends. */
public interface VirtualChunk {

    int chunkX();

    int chunkZ();
}
