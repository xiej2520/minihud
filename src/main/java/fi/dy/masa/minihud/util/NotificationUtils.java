package fi.dy.masa.minihud.util;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class NotificationUtils
{
    public static void onBlockChange(BlockPos pos, BlockState stateNew)
    {
        DataStorage.getInstance().markChunkForHeightmapCheck(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static void onMultiBlockChange(ChunkPos chunkPos, ChunkDeltaUpdateS2CPacket packet)
    {
        DataStorage.getInstance().markChunkForHeightmapCheck(chunkPos.x, chunkPos.z);
    }

    public static void onChunkData(int chunkX, int chunkZ, ChunkDataS2CPacket packet)
    {
        DataStorage.getInstance().markChunkForHeightmapCheck(chunkX, chunkZ);
    }
}
