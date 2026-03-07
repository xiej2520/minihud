package minihud.renderer;

import malilib.render.ShapeRenderUtils;
import malilib.util.data.Color4f;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3d;
import minihud.config.Configs;
import minihud.config.RendererToggle;
import minihud.data.DataStorage;
import net.minecraft.entity.Entity;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

public class OverlayRendererSimulationDistance extends MiniHudOverlayRenderer
{
    protected final RendererToggle toggle;

    protected BlockPos pos = BlockPos.ORIGIN;
    @Nullable public BlockPos newPos;

    protected OverlayRendererSimulationDistance(RendererToggle toggle)
    {
        this.toggle = toggle;
    }

    @Override
    public void onEnabled()
    {
        super.onEnabled();

        this.newPos = EntityWrap.getCameraEntityBlockPos();
        this.setNeedsUpdate();
    }

    @Override
    public boolean shouldRender()
    {
        return this.toggle.isRendererEnabled();
    }

    @Override
    public boolean needsUpdate(Entity entity)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        return this.newPos != null;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        DataStorage data = DataStorage.getInstance();

        if (this.newPos != null)
        {
            this.pos = this.newPos;
            this.newPos = null;
        }

        // TODO: fetch view distance from server
        //if (data.isSimulationDistanceKnown())
        {
            int viewDistance = GameWrap.getRenderDistanceChunks();

            int entityTicking = viewDistance - 2;

            int blockTicking = viewDistance;

            this.startBuffers();

            final Color4f colorEntity = Configs.Colors.SIMULATION_DISTANCE_ENTITY_TICKING_OVERLAY_COLOR.getColor();
            final Color4f colorLazy = Configs.Colors.SIMULATION_DISTANCE_BLOCK_TICKING_OVERLAY_COLOR.getColor();

            ShapeRenderUtils.renderBlockPosSideQuads(this.pos, 0.001, colorEntity, cameraPos, this.quadBuilder);
            ShapeRenderUtils.renderBlockPosEdgeLines(this.pos, 0.001, colorEntity, cameraPos, this.lineBuilder);

            Pair<BlockPos, BlockPos> corners = this.getViewDistanceCorners(this.pos, blockTicking * 16);
            RenderUtils.renderWallsWithLines(corners.getLeft(), corners.getRight(), cameraPos, 16, 16,
                true, colorLazy, this.quadBuilder, this.lineBuilder);

            corners = this.getViewDistanceCorners(this.pos, entityTicking * 16);
            RenderUtils.renderWallsWithLines(corners.getLeft(), corners.getRight(), cameraPos, 16, 16,
                true, colorEntity, this.quadBuilder, this.lineBuilder);

            this.uploadBuffers();
            this.needsUpdate = false;
        }
    }

    protected Pair<BlockPos, BlockPos> getViewDistanceCorners(BlockPos pos, int viewDistanceBlocks)
    {
        int x;
        int z;
        x = (pos.getX() & ~0xF) - viewDistanceBlocks;
        z = (pos.getZ() & ~0xF) - viewDistanceBlocks;
        BlockPos pos1 = new BlockPos(x, 0, z);

        x = (pos.getX() & ~0xF) + viewDistanceBlocks + 16 - 1;
        z = (pos.getZ() & ~0xF) + viewDistanceBlocks + 16 - 1;
        BlockPos pos2 = new BlockPos(x, 256, z);

        return Pair.of(pos1, pos2);
    }
}
