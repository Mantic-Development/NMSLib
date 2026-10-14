package me.fullpage.nmslib.chunk;

/** Receives the block entities of a chunk source. */
public interface BlockEntitySink {

    void accept(int x, int y, int z, String snbt);
}
