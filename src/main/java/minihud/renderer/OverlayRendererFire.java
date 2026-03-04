package minihud.renderer;

import malilib.render.ShapeRenderUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.Vec3d;
import minihud.config.Configs;
import minihud.config.RendererToggle;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.HashSet;
import java.util.Set;

public class OverlayRendererFire extends MiniHudOverlayRenderer
{
    private final Set<BlockPos> firePositions = new HashSet<>();
    private final Set<ChunkPos> scannedChunks = new HashSet<>();

    public OverlayRendererFire() {
        super();
        this.setDisableDepthTest(true);
    }

    public void clear()
    {
        synchronized (this.firePositions)
        {
            this.firePositions.clear();
            this.scannedChunks.clear();
        }
    }

    @Override
    public void setNeedsUpdate()
    {
        super.setNeedsUpdate();

        if (RendererToggle.FIRE.isRendererEnabled() == false)
        {
            this.clear();
        }
    }

    @Override
    public boolean shouldRender()
    {
        return RendererToggle.FIRE.isRendererEnabled();
    }

    @Override
    public boolean needsUpdate(Entity entity)
    {
        return this.needsUpdate || this.lastUpdatePos == null;
    }

    public void checkNeedsUpdate(BlockPos pos, IBlockState state)
    {
        synchronized (this.firePositions)
        {
            if (RendererToggle.FIRE.isRendererEnabled())
            {
                if (state.getBlock() == Blocks.FIRE)
                {
                    this.firePositions.add(pos);
                    this.setNeedsUpdate();
                }
                else if (this.firePositions.contains(pos))
                {
                    this.firePositions.remove(pos);
                    this.setNeedsUpdate();
                }
            }
        }
    }

    public void checkNeedsUpdate(ChunkPos chunkPos)
    {
        if (RendererToggle.FIRE.isRendererEnabled())
        {
            scanChunkForFire(chunkPos);
        }
    }

    private void scanChunkForFire(ChunkPos chunkPos)
    {
        synchronized (this.scannedChunks)
        {
            if (this.scannedChunks.contains(chunkPos))
            {
                return;
            }

            Chunk chunk = GameWrap.getClientWorld().getChunk(chunkPos.getX(), chunkPos.getZ());

            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    for (int y = 0; y <= chunk.getHeightValue(x, z); y++)
                    {
                        net.minecraft.util.math.BlockPos pos = chunkPos.getBlock(x, y, z);
                        IBlockState state = chunk.getBlockState(pos);
                        if (state.getBlock().equals(Blocks.FIRE))
                        {
                            this.firePositions.add(BlockPos.of(pos));
                            this.setNeedsUpdate();
                        }
                    }
                }
            }
            this.scannedChunks.add(chunkPos);
        }
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        this.startBuffers();

        final int centerX = EntityWrap.getChunkX(entity);
        final int centerZ = EntityWrap.getChunkZ(entity);

        synchronized (this.firePositions)
        {
            int r = GameWrap.getRenderDistanceChunks();

            for (int xOff = -r; xOff <= r; xOff++)
            {
                for (int zOff = -r; zOff <= r; zOff++)
                {
                    int cx = centerX + xOff;
                    int cz = centerZ + zOff;
                    scanChunkForFire(new ChunkPos(cx, cz));
                }
            }

            this.renderFire(GameWrap.getClientWorld(), cameraPos);
        }

        this.uploadBuffers();
    }

    protected void renderFire(World world, Vec3d cameraPos)
    {
        for (BlockPos pos : this.firePositions)
        {
            ShapeRenderUtils.renderBlockPosSideQuads(pos, 0.001, Configs.Colors.FIRE_POSITIONS_OVERLAY_COLOR.getColor(), cameraPos, this.quadBuilder);
            ShapeRenderUtils.renderBlockPosEdgeLines(pos, 0.001, Configs.Colors.FIRE_POSITIONS_OVERLAY_COLOR.getColor(), cameraPos, this.lineBuilder);
        }
    }

}
