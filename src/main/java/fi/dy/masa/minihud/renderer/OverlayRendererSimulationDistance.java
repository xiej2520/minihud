package fi.dy.masa.minihud.renderer;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OverlayRendererSimulationDistance extends OverlayRendererBase implements AutoCloseable
{
    public static final OverlayRendererSimulationDistance INSTANCE = new OverlayRendererSimulationDistance(RendererToggle.OVERLAY_SIMULATION_DISTANCE);
    protected final RendererToggle toggle;
    protected boolean needsUpdate = true;

    protected List<AABB> boxesOuter;
    protected List<AABB> boxesBorder;
    protected List<AABB> boxesBlockTicking;
    protected List<AABB> boxesEntityTicking;
    private boolean hasData;

    protected BlockPos pos = BlockPos.ZERO;
    @Nullable public BlockPos newPos;

    protected OverlayRendererSimulationDistance(RendererToggle toggle)
    {
        this.toggle = toggle;
        this.boxesOuter = new ArrayList<>();
        this.boxesBorder = new ArrayList<>();
        this.boxesBlockTicking = new ArrayList<>();
        this.boxesEntityTicking = new ArrayList<>();
        this.useCulling = false;
        this.hasData = false;
    }

    @Override
    public String getName()
    {
        return "SimulationDistance";
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    public void setNewPos(@Nullable BlockPos pos)
    {
        this.newPos = pos;
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return this.toggle.getBooleanValue() && DataStorage.getInstance().isSimulationDistanceKnown();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        return this.newPos != null;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null || !RenderSystem.isOnRenderThread())
        {
            return;
        }

        DataStorage data = DataStorage.getInstance();

        if (this.newPos != null)
        {
            this.pos = this.newPos;
            this.newPos = null;
        }

        if (data.isSimulationDistanceKnown())
        {
            int simulationDistance = data.getSimulationDistance();

            // entity ticking: chunk ticket <= 31
            int entityTicking = simulationDistance;

            // block ticking: chunk ticket <= 32
            int blockTicking = simulationDistance + 1;

            // full chunk (border): chunk ticket <= 33
            int border = simulationDistance + 2;

            // inaccessible (world generation): chunk ticket < 44
            int outer = simulationDistance + 13;

            boolean outerEnabled = Configs.Generic.SIMULATION_DISTANCE_OUTER_OVERLAY_ENABLED.getBooleanValue();
            boolean blockTickingEnabled = Configs.Generic.SIMULATION_DISTANCE_BLOCK_TICKING_OVERLAY_ENABLED.getBooleanValue();
            Pair<BlockPos, BlockPos> corners;
            if (outerEnabled)
            {
                corners = this.getChunkRangeCorners(this.pos, outer, mc.level);
                this.boxesOuter = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
            }

            corners = this.getChunkRangeCorners(this.pos, border, mc.level);
            this.boxesBorder = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());

            if (blockTickingEnabled)
            {
                corners = this.getChunkRangeCorners(this.pos, blockTicking, mc.level);
                this.boxesBlockTicking = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
            }

            corners = this.getChunkRangeCorners(this.pos, entityTicking, mc.level);
            this.boxesEntityTicking = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        this.hasData = true;
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.pos != null;
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("simulation_distance_quads");
        final Color4f colorEntity = Configs.Colors.SIMULATION_DISTANCE_ENTITY_TICKING_OVERLAY_COLOR.getColor();
        final Color4f colorBlock = Configs.Colors.SIMULATION_DISTANCE_BLOCK_TICKING_OVERLAY_COLOR.getColor();
        final Color4f colorBorder = Configs.Colors.SIMULATION_DISTANCE_BORDER_OVERLAY_COLOR.getColor();
        final Color4f colorOuter = Configs.Colors.SIMULATION_DISTANCE_OUTER_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:simulation_distance/quads", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

        if (DataStorage.getInstance().isSimulationDistanceKnown())
        {
            for (AABB entry : this.boxesOuter)
            {
                RenderUtils.renderWallQuads(entry, cameraPos, colorOuter, builder);
            }
            for (AABB entry : this.boxesBorder)
            {
                RenderUtils.renderWallQuads(entry, cameraPos, colorBorder, builder);
            }
            for (AABB entry : this.boxesBlockTicking)
            {
                RenderUtils.renderWallQuads(entry, cameraPos, colorBlock, builder);
            }
            for (AABB entry : this.boxesEntityTicking)
            {
                RenderUtils.renderWallQuads(entry, cameraPos, colorEntity, builder);
            }
        }

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, this.shouldResort);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSimulationDistance#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("simulation_distance_outlines");
        Color4f colorEntity = Configs.Colors.SIMULATION_DISTANCE_ENTITY_TICKING_OVERLAY_COLOR.getColor();
        Color4f colorBlock = Configs.Colors.SIMULATION_DISTANCE_BLOCK_TICKING_OVERLAY_COLOR.getColor();
        Color4f colorBorder = Configs.Colors.SIMULATION_DISTANCE_BORDER_OVERLAY_COLOR.getColor();
        Color4f colorOuter = Configs.Colors.SIMULATION_DISTANCE_OUTER_OVERLAY_COLOR.getColor();

        // Solid Lines
        colorEntity = Color4f.fromColor(colorEntity, 0xFF);
        colorBlock = Color4f.fromColor(colorBlock, 0xFF);
        colorBorder = Color4f.fromColor(colorBorder, 0xFF);
        colorOuter = Color4f.fromColor(colorOuter, 0xFF);

        final float lineWidth = 3.0f;
        this.glLineWidth = lineWidth;

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:simulation_distance/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        if (DataStorage.getInstance().isSimulationDistanceKnown())
        {
            for (AABB entry : this.boxesOuter)
            {
                RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorOuter, lineWidth, builder);
            }
            for (AABB entry : this.boxesBorder)
            {
                RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorBorder, lineWidth, builder);
            }
            for (AABB entry : this.boxesBlockTicking)
            {
                RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorBlock, lineWidth, builder);
            }
            for (AABB entry : this.boxesEntityTicking)
            {
                RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorEntity, lineWidth, builder);
            }
        }

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.boxesOuter.clear();
        this.boxesBorder.clear();
        this.boxesBlockTicking.clear();
        this.boxesEntityTicking.clear();
        this.pos = BlockPos.ZERO;
        this.hasData = false;
    }

    @Override
    public void close()
    {
        this.reset();
    }

    protected Pair<BlockPos, BlockPos> getChunkRangeCorners(BlockPos pos, int chunkRange, Level world)
    {
        int cx = (pos.getX() >> 4);
        int cz = (pos.getZ() >> 4);

        int minY = world != null ? world.getMinY() : -64;
        int maxY = world != null ? world.getMaxY() + 1 : 320;
        BlockPos pos1 = new BlockPos( (cx - chunkRange) << 4      , minY,  (cz - chunkRange) << 4);
        BlockPos pos2 = new BlockPos(((cx + chunkRange) << 4) + 15, maxY, ((cz + chunkRange) << 4) + 15);

        return Pair.of(pos1, pos2);
    }

    @Nullable
    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (!this.pos.equals(BlockPos.ZERO))
        {
            obj.add("pos", JsonUtils.blockPosToJson(this.pos));
        }

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        if (obj.has("pos"))
        {
            BlockPos pos = JsonUtils.getBlockPos(obj, "pos");

            if (pos != null)
            {
                newPos = pos;
            }
        }
    }
}
