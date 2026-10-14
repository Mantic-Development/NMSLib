package me.fullpage.nmslib.v26_2;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

/** Light properties per state id, built on first use; class initialization publishes them to every thread. */
final class LightTables {

    static final byte[] EMISSION;
    static final byte[] OPACITY;
    static final byte[] OCCLUSION_FACES;
    static final boolean[] SKYLIGHT_DOWN;

    static {
        int count = Block.BLOCK_STATE_REGISTRY.size();
        EMISSION = new byte[count];
        OPACITY = new byte[count];
        OCCLUSION_FACES = new byte[count];
        SKYLIGHT_DOWN = new boolean[count];
        Direction[] directions = Direction.values();
        for (int id = 0; id < count; id++) {
            BlockState state = Block.BLOCK_STATE_REGISTRY.byId(id);
            EMISSION[id] = (byte) state.getLightEmission();
            OPACITY[id] = (byte) state.getLightDampening();
            SKYLIGHT_DOWN[id] = state.propagatesSkylightDown();
            // shortcut: vanilla shapeOccludes against an empty neighbour face, so partial faces that only cover a face together (bottom slab beside top slab) are missed; expose face shapes if schematics need it.
            if (!state.canOcclude() || !state.useShapeForLightOcclusion()) continue;
            int faces = 0;
            for (Direction direction : directions) {
                if (Shapes.faceShapeOccludes(Shapes.empty(), state.getFaceOcclusionShape(direction))) faces |= 1 << direction.ordinal();
            }
            OCCLUSION_FACES[id] = (byte) faces;
        }
    }

    private LightTables() {
    }
}
