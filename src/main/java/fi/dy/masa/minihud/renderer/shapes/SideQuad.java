package fi.dy.masa.minihud.renderer.shapes;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public final class SideQuad {
    private final long startPos;
    private final int width;
    private final int height;
    private final Direction side;

    public SideQuad(long startPos, int width, int height, Direction side) {
        this.startPos = startPos;
        this.width = width;
        this.height = height;
        this.side = side;
    }

    @Override
    public String toString() {
        return "SideQuad{start=" + String.format("BlockPos{x=%d,y=%d,z=%d}",
                BlockPos.unpackLongX(this.startPos),
                BlockPos.unpackLongY(this.startPos),
                BlockPos.unpackLongZ(this.startPos)) +
                ", width=" + this.width +
                ", height=" + this.height +
                ", side=" + this.side + '}';
    }

    public long startPos() {
        return startPos;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Direction side() {
        return side;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        SideQuad that = (SideQuad) obj;
        return this.startPos == that.startPos &&
                this.width == that.width &&
                this.height == that.height &&
                Objects.equals(this.side, that.side);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPos, width, height, side);
    }

}
