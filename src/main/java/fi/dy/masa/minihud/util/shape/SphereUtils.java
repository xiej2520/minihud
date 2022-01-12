package fi.dy.masa.minihud.util.shape;

import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SphereUtils
{
    public static boolean movePositionToRing(BlockPos.Mutable posMutable,
                                             Direction moveDirection,
                                             double radius,
                                             RingPositionTest test)
    {
        final int failsafeMax = (int) radius + 2;
        final int incX = moveDirection.getOffsetX();
        final int incY = moveDirection.getOffsetY();
        final int incZ = moveDirection.getOffsetZ();
        int x = posMutable.getX();
        int y = posMutable.getY();
        int z = posMutable.getZ();
        int nextX = x;
        int nextY = y;
        int nextZ = z;
        int failsafe = 0;

        while (test.isInsideOrCloserThan(nextX, nextY, nextZ, moveDirection) && ++failsafe < failsafeMax)
        {
            x = nextX;
            y = nextY;
            z = nextZ;
            nextX += incX;
            nextY += incY;
            nextZ += incZ;
        }

        // Successfully entered the loop at least once
        if (failsafe > 0)
        {
            posMutable.set(x, y, z);
            return true;
        }

        return false;
    }

    public static void addPositionsOnHorizontalBlockRing(Consumer<BlockPos.Mutable> positionConsumer,
                                                         BlockPos.Mutable mutablePos,
                                                         RingPositionTest test,
                                                         double radius)
    {
        Function<Direction, Direction> nextDirectionFunction = SphereUtils::getNextHorizontalDirection;
        Direction startDirection = Direction.EAST;
        addPositionsOnBlockRing(positionConsumer, mutablePos, startDirection, test, nextDirectionFunction, radius);
    }

    public static void addPositionsOnVerticalBlockRing(Consumer<BlockPos.Mutable> positionConsumer,
                                                       BlockPos.Mutable mutablePos,
                                                       Direction mainAxis,
                                                       RingPositionTest test,
                                                       double radius)
    {
        Function<Direction, Direction> nextDirectionFunction = (dir) -> SphereUtils.getNextVerticalRingDirection(dir, mainAxis);
        Direction startDirection = Direction.UP;
        addPositionsOnBlockRing(positionConsumer, mutablePos, startDirection, test, nextDirectionFunction, radius);
    }

    public static void addPositionsOnBlockRing(Consumer<BlockPos.Mutable> positionConsumer,
                                               BlockPos.Mutable mutablePos,
                                               Direction startDirection,
                                               RingPositionTest test,
                                               Function<Direction, Direction> nextDirectionFunction,
                                               double radius)
    {
        if (movePositionToRing(mutablePos, startDirection, radius, test))
        {
            final BlockPos firstPos = mutablePos.toImmutable();
            Direction direction = startDirection;
            int failsafe = (int) (2.5 * Math.PI * radius); // a bit over the circumference

            positionConsumer.accept(mutablePos);

            while (--failsafe > 0)
            {
                direction = getNextPositionOnBlockRing(mutablePos, direction, test, nextDirectionFunction);

                if (direction == null || mutablePos.equals(firstPos))
                {
                    break;
                }

                positionConsumer.accept(mutablePos);
            }
        }
    }

    @Nullable
    public static Direction getNextPositionOnBlockRing(BlockPos.Mutable posMutable,
                                                       Direction escapeDirection,
                                                       RingPositionTest test,
                                                       Function<Direction, Direction> nextDirectionFunction)
    {
        Direction dirOut = escapeDirection;
        Direction ccw90;

        for (int i = 0; i < 4; ++i)
        {
            int x = posMutable.getX() + escapeDirection.getOffsetX();
            int y = posMutable.getY() + escapeDirection.getOffsetY();
            int z = posMutable.getZ() + escapeDirection.getOffsetZ();

            // First check the adjacent position
            if (test.isInsideOrCloserThan(x, y, z, escapeDirection))
            {
                posMutable.set(x, y, z);
                return dirOut;
            }

            ccw90 = nextDirectionFunction.apply(escapeDirection);

            // Then check the diagonal position
            x += ccw90.getOffsetX();
            y += ccw90.getOffsetY();
            z += ccw90.getOffsetZ();

            if (test.isInsideOrCloserThan(x, y, z, escapeDirection))
            {
                posMutable.set(x, y, z);
                return dirOut;
            }

            // Delay the next direction by one cycle, so that it won't get updated too soon on the diagonals
            dirOut = escapeDirection;
            escapeDirection = nextDirectionFunction.apply(escapeDirection);
        }

        return null;
    }

    public static boolean isPositionInsideOrClosestToRadiusOnBlockRing(int blockX,
                                                                       int blockY,
                                                                       int blockZ,
                                                                       Vec3d center,
                                                                       double squareRadius,
                                                                       Direction escapeDirection)
    {
        double x = (double) blockX + 0.5;
        double y = (double) blockY + 0.5;
        double z = (double) blockZ + 0.5;
        double dist = center.squaredDistanceTo(x, y, z);
        double diff = squareRadius - dist;

        if (diff > 0)
        {
            return true;
        }

        double xAdj = (double) blockX + escapeDirection.getOffsetX() + 0.5;
        double yAdj = (double) blockY + escapeDirection.getOffsetY() + 0.5;
        double zAdj = (double) blockZ + escapeDirection.getOffsetZ() + 0.5;
        double distAdj = center.squaredDistanceTo(xAdj, yAdj, zAdj);
        double diffAdj = squareRadius - distAdj;

        return diffAdj > 0 && Math.abs(diff) < Math.abs(diffAdj);
    }

    /**
     * Returns the next horizontal direction in sequence, rotating counter-clockwise
     */
    protected static Direction getNextHorizontalDirection(Direction dirIn)
    {
        return dirIn.rotateYCounterclockwise();
    }

    /**
     * Returns the next direction in sequence, rotating up to north
     */
    protected static Direction getNextVerticalRingDirection(Direction currentDirection, Direction mainAxis)
    {
         switch (mainAxis) {
             case UP:
             case DOWN:
                 switch (currentDirection) {
                     case NORTH: return Direction.DOWN;
                     case SOUTH: return Direction.UP;
                     case DOWN:  return Direction.SOUTH;
                     default:    return Direction.NORTH;
                 }
             case NORTH:
             case SOUTH:
                 switch (currentDirection) {
                     case WEST: return Direction.UP;
                     case EAST: return Direction.DOWN;
                     case DOWN: return Direction.WEST;
                     default:   return Direction.EAST;
                 }
             case WEST:
             case EAST:
                 switch (currentDirection) {
                     case NORTH: return Direction.UP;
                     case SOUTH: return Direction.DOWN;
                     case DOWN:  return Direction.NORTH;
                     default:    return Direction.SOUTH;
                 }
        };
        // unreachable
        return Direction.UP;
    }

    public interface RingPositionTest
    {
        boolean isInsideOrCloserThan(int x, int y, int z, Direction outsideDirection);
    }
}
