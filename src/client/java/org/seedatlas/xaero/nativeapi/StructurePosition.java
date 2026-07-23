package org.seedatlas.xaero.nativeapi;

/**
 * A structure start or helper marker. Detail is type-specific (currently the
 * stronghold ring or fixed-gateway order) and zero for ordinary starts.
 */
public record StructurePosition(
    StructureType type,
    int x,
    int y,
    int z,
    Viability viability,
    boolean approximate,
    int detail
) {
    public StructurePosition {
        if (type == null || viability == null) {
            throw new NullPointerException("type and viability are required");
        }
    }

    public BlockPos position() {
        return new BlockPos(x, y, z);
    }
}
