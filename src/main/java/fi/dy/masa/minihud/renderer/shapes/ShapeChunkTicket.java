package fi.dy.masa.minihud.renderer.shapes;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.util.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.RenderObjectVbo;
import fi.dy.masa.minihud.renderer.RenderUtils;
import fi.dy.masa.minihud.util.DataStorage;

public class ShapeChunkTicket extends ShapeBase
{
    @NotNull protected BlockPos pos = BlockPos.ZERO;
    protected int radius;

    @NotNull protected List<AABB> boxesEntityTicking;
    @NotNull protected List<AABB> boxesBlockTicking;
    @NotNull protected List<AABB> boxesBorder;
    @NotNull protected List<AABB> boxesOuter;

    boolean entityTickingEnabled = true;
    boolean blockTickingEnabled = false;
    boolean borderEnabled = true;
    boolean outerEnabled = false;

    private boolean hasData;

    public ShapeChunkTicket()
    {
        this(ShapeType.CHUNK_TICKET, Color4f.WHITE);
    }

    public ShapeChunkTicket(ShapeType type, Color4f color)
    {
        super(type, color);
        this.boxesOuter = new ArrayList<>();
        this.boxesBorder = new ArrayList<>();
        this.boxesBlockTicking = new ArrayList<>();
        this.boxesEntityTicking = new ArrayList<>();
        this.radius = DataStorage.getInstance().getSimulationDistance();
    }

	@Override
	public void onShapeInit()
	{
		Entity cameraEntity = EntityUtils.getCameraEntity();

		if (cameraEntity != null && this.pos == BlockPos.ZERO)
		{
            this.pos = cameraEntity.blockPosition();
		}
	}

    public @NotNull BlockPos getPos()
    {
        return this.pos;
    }

    public void setPos(@NotNull BlockPos pos)
    {
        this.pos = pos;
        this.setNeedsUpdate();
    }

    public int getRadius()
    {
        return this.radius;
    }

    public void setRadius(int radius)
    {
        this.radius = radius;
        this.setNeedsUpdate();
    }

    public int getTicketLevel()
    {
        // TicketStorage#addTicketWithRadius
        return ChunkLevel.byStatus(FullChunkStatus.FULL) - this.radius;
    }

    public void setTicketLevel(int ticketLevel)
    {
        this.setRadius(ChunkLevel.byStatus(FullChunkStatus.FULL) - ticketLevel);
    }

    public boolean isEntityTickingEnabled()
    {
        return this.entityTickingEnabled;
    }

    public void setEntityTickingEnabled(boolean entityTickingEnabled)
    {
        this.entityTickingEnabled = entityTickingEnabled;
        this.setNeedsUpdate();
    }

    public boolean isBlockTickingEnabled()
    {
        return this.blockTickingEnabled;
    }

    public void setBlockTickingEnabled(boolean blockTickingEnabled)
    {
        this.blockTickingEnabled = blockTickingEnabled;
        this.setNeedsUpdate();
    }

    public boolean isBorderEnabled()
    {
        return this.borderEnabled;
    }

    public void setBorderEnabled(boolean borderEnabled)
    {
        this.borderEnabled = borderEnabled;
        this.setNeedsUpdate();
    }

    public boolean isOuterEnabled()
    {
        return this.outerEnabled;
    }

    public void setOuterEnabled(boolean outerEnabled)
    {
        this.outerEnabled = outerEnabled;
        this.setNeedsUpdate();
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        Entity entity = EntityUtils.getCameraEntity();
        return super.shouldRender(mc) && entity != null;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        // entity ticking: chunk ticket <= 31
        int entityTicking = this.radius;

        // block ticking: chunk ticket <= 32
        int blockTicking = this.radius + 1;

        // full chunk (border): chunk ticket <= 33
        int border = this.radius + 2;

        // inaccessible (world generation): chunk ticket < 44
        int outer = this.radius + 13;

        this.boxesEntityTicking.clear();
        this.boxesBlockTicking.clear();
        this.boxesBorder.clear();
        this.boxesOuter.clear();

        Pair<BlockPos, BlockPos> corners;
        if (this.outerEnabled)
        {
            corners = this.getChunkRangeCorners(this.pos, outer, mc.level);
            this.boxesOuter = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        if (this.borderEnabled)
        {
            corners = this.getChunkRangeCorners(this.pos, border, mc.level);
            this.boxesBorder = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        if (this.blockTickingEnabled)
        {
            corners = this.getChunkRangeCorners(this.pos, blockTicking, mc.level);
            this.boxesBlockTicking = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        if (this.entityTickingEnabled)
        {
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
        return this.hasData;
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        if (this.shouldRenderLines())
        {
            this.renderOutlines(cameraPos, mc, profiler);
        }
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("chunk_ticket_quads");
        final Color4f colorEntity = Configs.Colors.SHAPE_CHUNK_TICKET_ENTITY_TICKING_OVERLAY_COLOR.getColor();
        final Color4f colorBlock = Configs.Colors.SHAPE_CHUNK_TICKET_BLOCK_TICKING_OVERLAY_COLOR.getColor();
        final Color4f colorBorder = Configs.Colors.SHAPE_CHUNK_TICKET_BORDER_OVERLAY_COLOR.getColor();
        final Color4f colorOuter = Configs.Colors.SHAPE_CHUNK_TICKET_OUTER_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:chunk_ticket/quads",  this.renderThroughShape ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

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
            MiniHUD.LOGGER.error("ShapeChunkTicket#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("chunk_ticket_outlines");
        Color4f colorEntity = Configs.Colors.SHAPE_CHUNK_TICKET_ENTITY_TICKING_OVERLAY_COLOR.getColor();
        Color4f colorBlock = Configs.Colors.SHAPE_CHUNK_TICKET_BLOCK_TICKING_OVERLAY_COLOR.getColor();
        Color4f colorBorder = Configs.Colors.SHAPE_CHUNK_TICKET_BORDER_OVERLAY_COLOR.getColor();
        Color4f colorOuter = Configs.Colors.SHAPE_CHUNK_TICKET_OUTER_OVERLAY_COLOR.getColor();

        // Solid Lines
        colorEntity = Color4f.fromColor(colorEntity, 0xFF);
        colorBlock = Color4f.fromColor(colorBlock, 0xFF);
        colorBorder = Color4f.fromColor(colorBorder, 0xFF);
        colorOuter = Color4f.fromColor(colorOuter, 0xFF);

        final float lineWidth = 3.0f;
        this.glLineWidth = lineWidth;

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:chunk_ticket/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

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
            MiniHUD.LOGGER.error("ShapeChunkTicket#renderOutlines(): Exception; {}", err.getMessage());
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
        this.hasData = false;
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        lines.add(StringUtils.translate("minihud.gui.label.shape.chunk_ticket.position_coords",
            this.pos.getX(), this.pos.getY(), this.pos.getZ(),
            this.pos.getX() >> 4, this.pos.getZ() >> 4
        ));
        lines.add(StringUtils.translate("minihud.gui.label.shape.chunk_ticket.radius", this.getRadius()));
        return lines;
    }

    protected Pair<BlockPos, BlockPos> getChunkRangeCorners(BlockPos pos, int chunkRange, Level world)
    {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        int minY = world != null ? world.getMinY() : -64;
        int maxY = world != null ? world.getMaxY() + 1 : 320;
        BlockPos pos1 = new BlockPos( (cx - chunkRange) << 4      , minY,  (cz - chunkRange) << 4);
        BlockPos pos2 = new BlockPos(((cx + chunkRange) << 4) + 15, maxY, ((cz + chunkRange) << 4) + 15);

        return Pair.of(pos1, pos2);
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        if (obj != null)
        {
            obj.add("pos", JsonUtils.blockPosToJson(this.pos));
            obj.addProperty("radius", this.radius);
        }

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        if (obj.has("pos"))
        {
            BlockPos pos = JsonUtils.getBlockPos(obj, "pos");
            if (pos != null)
            {
                this.setPos(pos);
            }
        }

        if (obj.has("radius"))
        {
            this.setRadius(JsonUtils.getInteger(obj, "radius"));
        }
    }
}
