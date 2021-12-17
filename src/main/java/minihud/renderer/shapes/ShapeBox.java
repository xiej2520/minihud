package minihud.renderer.shapes;

import java.util.List;
import com.google.gson.JsonObject;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.Direction;
import malilib.util.position.PositionUtils;
import malilib.util.position.Vec3d;
import minihud.config.Configs;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

public class ShapeBox extends ShapeBase
{
    protected static final AxisAlignedBB EMPTY_BOX = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

    protected AxisAlignedBB box = EMPTY_BOX;
    protected AxisAlignedBB renderPerimeter = EMPTY_BOX;
    protected int enabledSidesMask = 0x3F;
    protected boolean gridEnabled = true;
    protected Vec3d gridSize = new Vec3d(16.0, 16.0, 16.0);
    protected Vec3d gridStartOffset = Vec3d.ZERO;
    protected Vec3d gridEndOffset = Vec3d.ZERO;

    public ShapeBox()
    {
        super(ShapeType.BOX, Configs.Colors.SHAPE_BOX.getColor());
    }

    public AxisAlignedBB getBox()
    {
        return this.box;
    }

    public int getEnabledSidesMask()
    {
        return this.enabledSidesMask;
    }

    public boolean isGridEnabled()
    {
        return this.gridEnabled;
    }

    public Vec3d getGridSize()
    {
        return this.gridSize;
    }

    public Vec3d getGridStartOffset()
    {
        return this.gridStartOffset;
    }

    public Vec3d getGridEndOffset()
    {
        return this.gridEndOffset;
    }

    public void setBox(AxisAlignedBB box)
    {
        this.box = box;

        double margin = GameWrap.getRenderDistanceChunks() * 16;
        this.renderPerimeter = box.expand(-margin, -margin, -margin).expand(margin, margin, margin);
        this.setNeedsUpdate();
    }

    public void setEnabledSidesMask(int enabledSidesMask)
    {
        this.enabledSidesMask = enabledSidesMask;
        this.setNeedsUpdate();
    }

    public void toggleGridEnabled()
    {
        this.gridEnabled = ! this.gridEnabled;
        this.setNeedsUpdate();
    }

    public void setGridSize(Vec3d gridSize)
    {
        double x = MathHelper.clamp(gridSize.x, 0.5, 1024);
        double y = MathHelper.clamp(gridSize.y, 0.5, 1024);
        double z = MathHelper.clamp(gridSize.z, 0.5, 1024);
        this.gridSize = new Vec3d(x, y, z);
        this.setNeedsUpdate();
    }

    public void setGridStartOffset(Vec3d gridStartOffset)
    {
        double x = MathHelper.clamp(gridStartOffset.x, 0.0, 1024);
        double y = MathHelper.clamp(gridStartOffset.y, 0.0, 1024);
        double z = MathHelper.clamp(gridStartOffset.z, 0.0, 1024);
        this.gridStartOffset = new Vec3d(x, y, z);
        this.setNeedsUpdate();
    }

    public void setGridEndOffset(Vec3d gridEndOffset)
    {
        double x = MathHelper.clamp(gridEndOffset.x, 0.0, 1024);
        double y = MathHelper.clamp(gridEndOffset.y, 0.0, 1024);
        double z = MathHelper.clamp(gridEndOffset.z, 0.0, 1024);
        this.gridEndOffset = new Vec3d(x, y, z);
        this.setNeedsUpdate();
    }

    @Override
    public boolean shouldRender()
    {
        Entity entity = GameWrap.getCameraEntity();
        return super.shouldRender() && entity != null && this.renderPerimeter.contains(entity.getPositionVector());
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        AxisAlignedBB box = this.box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        this.renderBox(box);

        this.needsUpdate = false;
        this.lastUpdatePos = EntityWrap.getEntityBlockPos(entity);
    }

    protected void renderBox(AxisAlignedBB box)
    {
        this.startBuffers();
        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            if (isSideEnabled(side, this.enabledSidesMask))
            {
                renderBoxSideQuad(box, side, this.color);
            }
        }

        Color4f color = Color4f.fromColor(this.color.intValue, 1f);
        renderBoxEnabledEdgeLines(box, color, this.enabledSidesMask);

        if (this.gridEnabled)
        {
            this.renderGridLines(box, color);
        }
        this.uploadBuffers();
    }

    protected void renderGridLines(AxisAlignedBB box, Color4f color)
    {
        if (isSideEnabled(Direction.DOWN, this.enabledSidesMask))
        {
            this.renderGridLinesY(box, box.minY, color);
        }

        if (isSideEnabled(Direction.UP, this.enabledSidesMask))
        {
            this.renderGridLinesY(box, box.maxY, color);
        }

        if (isSideEnabled(Direction.NORTH, this.enabledSidesMask))
        {
            this.renderGridLinesZ(box, box.minZ, color);
        }

        if (isSideEnabled(Direction.SOUTH, this.enabledSidesMask))
        {
            this.renderGridLinesZ(box, box.maxZ, color);
        }

        if (isSideEnabled(Direction.WEST, this.enabledSidesMask))
        {
            this.renderGridLinesX(box, box.minX, color);
        }

        if (isSideEnabled(Direction.EAST, this.enabledSidesMask))
        {
            this.renderGridLinesX(box, box.maxX, color);
        }
    }

    protected void renderGridLinesX(AxisAlignedBB box, double x, Color4f color)
    {
        double end = box.maxY - this.gridEndOffset.y;
        double min = box.minZ + this.gridStartOffset.z;
        double max = box.maxZ - this.gridEndOffset.z;

        for (double y = box.minY + this.gridStartOffset.y; y <= end; y += this.gridSize.y)
        {
            this.lineBuilder.posColor(x, y, min, color);
            this.lineBuilder.posColor(x, y, max, color);
        }

        end = box.maxZ - this.gridEndOffset.z;
        min = box.minY + this.gridStartOffset.y;
        max = box.maxY - this.gridEndOffset.y;

        for (double z = box.minZ + this.gridStartOffset.z; z <= end; z += this.gridSize.z)
        {
            this.lineBuilder.posColor(x, min, z, color);
            this.lineBuilder.posColor(x, max, z, color);
        }
    }

    protected void renderGridLinesY(AxisAlignedBB box, double y, Color4f color)
    {
        double end = box.maxX - this.gridEndOffset.x;
        double min = box.minZ + this.gridStartOffset.z;
        double max = box.maxZ - this.gridEndOffset.z;

        for (double x = box.minX + this.gridStartOffset.x; x <= end; x += this.gridSize.x)
        {
            this.lineBuilder.posColor(x, y, min, color);
            this.lineBuilder.posColor(x, y, max, color);
        }

        end = box.maxZ - this.gridEndOffset.z;
        min = box.minX + this.gridStartOffset.x;
        max = box.maxX - this.gridEndOffset.x;

        for (double z = box.minZ + this.gridStartOffset.z; z <= end; z += this.gridSize.z)
        {
            this.lineBuilder.posColor(min, y, z, color);
            this.lineBuilder.posColor(max, y, z, color);
        }
    }

    protected void renderGridLinesZ(AxisAlignedBB box, double z, Color4f color)
    {
        double end = box.maxX - this.gridEndOffset.x;
        double min = box.minY + this.gridStartOffset.y;
        double max = box.maxY - this.gridEndOffset.y;

        for (double x = box.minX + this.gridStartOffset.x; x <= end; x += this.gridSize.x)
        {
            this.lineBuilder.posColor(x, min, z, color);
            this.lineBuilder.posColor(x, max, z, color);
        }

        end = box.maxY - this.gridEndOffset.y;
        min = box.minX + this.gridStartOffset.x;
        max = box.maxX - this.gridEndOffset.x;

        for (double y = box.minY + this.gridStartOffset.y; y <= end; y += this.gridSize.y)
        {
            this.lineBuilder.posColor(min, y, z, color);
            this.lineBuilder.posColor(max, y, z, color);
        }
    }

    public boolean isSideEnabled(Direction side)
    {
        return isSideEnabled(side, this.enabledSidesMask);
    }

    public static boolean isSideEnabled(Direction side, int enabledSidesMask)
    {
        return (enabledSidesMask & (1 << side.getIndex())) != 0;
    }

    // TODO: move to ShapeRenderUtils?
    private void renderBoxSideQuad(AxisAlignedBB box, Direction side, Color4f color)
    {
        switch (side)
        {
            case DOWN:
                this.quadBuilder.posColor(box.minX, box.minY, box.minZ, color);
                this.quadBuilder.posColor(box.maxX, box.minY, box.minZ, color);
                this.quadBuilder.posColor(box.maxX, box.minY, box.maxZ, color);
                this.quadBuilder.posColor(box.minX, box.minY, box.maxZ, color);
                break;

            case UP:
                this.quadBuilder.posColor(box.minX, box.maxY, box.minZ, color);
                this.quadBuilder.posColor(box.minX, box.maxY, box.maxZ, color);
                this.quadBuilder.posColor(box.maxX, box.maxY, box.maxZ, color);
                this.quadBuilder.posColor(box.maxX, box.maxY, box.minZ, color);
                break;

            case NORTH:
                this.quadBuilder.posColor(box.minX, box.minY, box.minZ, color);
                this.quadBuilder.posColor(box.minX, box.maxY, box.minZ, color);
                this.quadBuilder.posColor(box.maxX, box.maxY, box.minZ, color);
                this.quadBuilder.posColor(box.maxX, box.minY, box.minZ, color);
                break;

            case SOUTH:
                this.quadBuilder.posColor(box.minX, box.minY, box.maxZ, color);
                this.quadBuilder.posColor(box.maxX, box.minY, box.maxZ, color);
                this.quadBuilder.posColor(box.maxX, box.maxY, box.maxZ, color);
                this.quadBuilder.posColor(box.minX, box.maxY, box.maxZ, color);
                break;

            case WEST:
                this.quadBuilder.posColor(box.minX, box.minY, box.minZ, color);
                this.quadBuilder.posColor(box.minX, box.minY, box.maxZ, color);
                this.quadBuilder.posColor(box.minX, box.maxY, box.maxZ, color);
                this.quadBuilder.posColor(box.minX, box.maxY, box.minZ, color);
                break;

            case EAST:
                this.quadBuilder.posColor(box.maxX, box.minY, box.minZ, color);
                this.quadBuilder.posColor(box.maxX, box.maxY, box.minZ, color);
                this.quadBuilder.posColor(box.maxX, box.maxY, box.maxZ, color);
                this.quadBuilder.posColor(box.maxX, box.minY, box.maxZ, color);
                break;
        }
    }

    private void renderBoxEnabledEdgeLines(AxisAlignedBB box, Color4f color, int enabledSidesMask)
    {
        boolean down  = isSideEnabled(Direction.DOWN,   enabledSidesMask);
        boolean up    = isSideEnabled(Direction.UP,     enabledSidesMask);
        boolean north = isSideEnabled(Direction.NORTH,  enabledSidesMask);
        boolean south = isSideEnabled(Direction.SOUTH,  enabledSidesMask);
        boolean west  = isSideEnabled(Direction.WEST,   enabledSidesMask);
        boolean east  = isSideEnabled(Direction.EAST,   enabledSidesMask);

        // Lines along the x-axis
        if (down || north)
        {
            this.lineBuilder.posColor(box.minX, box.minY, box.minZ, color);
            this.lineBuilder.posColor(box.maxX, box.minY, box.minZ, color);
        }

        if (up || north)
        {
            this.lineBuilder.posColor(box.minX, box.maxY, box.minZ, color);
            this.lineBuilder.posColor(box.maxX, box.maxY, box.minZ, color);
        }

        if (down || south)
        {
            this.lineBuilder.posColor(box.minX, box.minY, box.maxZ, color);
            this.lineBuilder.posColor(box.maxX, box.minY, box.maxZ, color);
        }

        if (up || south)
        {
            this.lineBuilder.posColor(box.minX, box.maxY, box.maxZ, color);
            this.lineBuilder.posColor(box.maxX, box.maxY, box.maxZ, color);
        }

        // Lines along the z-axis
        if (down || west)
        {
            this.lineBuilder.posColor(box.minX, box.minY, box.minZ, color);
            this.lineBuilder.posColor(box.minX, box.minY, box.maxZ, color);
        }

        if (up || west)
        {
            this.lineBuilder.posColor(box.minX, box.maxY, box.minZ, color);
            this.lineBuilder.posColor(box.minX, box.maxY, box.maxZ, color);
        }

        if (down || east)
        {
            this.lineBuilder.posColor(box.maxX, box.minY, box.minZ, color);
            this.lineBuilder.posColor(box.maxX, box.minY, box.maxZ, color);
        }

        if (up || east)
        {
            this.lineBuilder.posColor(box.maxX, box.maxY, box.minZ, color);
            this.lineBuilder.posColor(box.maxX, box.maxY, box.maxZ, color);
        }

        // Lines along the y-axis
        if (north || west)
        {
            this.lineBuilder.posColor(box.minX, box.minY, box.minZ, color);
            this.lineBuilder.posColor(box.minX, box.maxY, box.minZ, color);
        }

        if (south || west)
        {
            this.lineBuilder.posColor(box.minX, box.minY, box.maxZ, color);
            this.lineBuilder.posColor(box.minX, box.maxY, box.maxZ, color);
        }

        if (north || east)
        {
            this.lineBuilder.posColor(box.maxX, box.minY, box.minZ, color);
            this.lineBuilder.posColor(box.maxX, box.maxY, box.minZ, color);
        }

        if (south || east)
        {
            this.lineBuilder.posColor(box.maxX, box.minY, box.maxZ, color);
            this.lineBuilder.posColor(box.maxX, box.maxY, box.maxZ, color);
        }
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        AxisAlignedBB box = this.box;
        lines.add(StringUtils.translate("minihud.label.shape.box.min_corner", box.minX, box.minY, box.minZ));
        lines.add(StringUtils.translate("minihud.label.shape.box.max_corner", box.maxX, box.maxY, box.maxZ));
        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        obj.addProperty("enabled_sides", this.enabledSidesMask);
        obj.addProperty("grid_enabled", this.gridEnabled);
        obj.add("grid_size",         JsonUtils.vec3dToJson(this.gridSize));
        obj.add("grid_start_offset", JsonUtils.vec3dToJson(this.gridStartOffset));
        obj.add("grid_end_offset",   JsonUtils.vec3dToJson(this.gridEndOffset));

        obj.addProperty("minX", this.box.minX);
        obj.addProperty("minY", this.box.minY);
        obj.addProperty("minZ", this.box.minZ);
        obj.addProperty("maxX", this.box.maxX);
        obj.addProperty("maxY", this.box.maxY);
        obj.addProperty("maxZ", this.box.maxZ);

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        this.enabledSidesMask = JsonUtils.getIntegerOrDefault(obj, "enabled_sides", 0x3F);
        this.gridEnabled     = JsonUtils.getBooleanOrDefault(obj, "grid_enabled", true);
        this.gridSize        = JsonUtils.getVec3d(obj, "grid_size");
        this.gridStartOffset = JsonUtils.getVec3d(obj, "grid_start_offset");
        this.gridEndOffset   = JsonUtils.getVec3d(obj, "grid_end_offset");

        if (this.gridSize == null)        { this.gridSize = new Vec3d(16.0, 16.0, 16.0); }
        if (this.gridStartOffset == null) { this.gridStartOffset = Vec3d.ZERO; }
        if (this.gridEndOffset == null)   { this.gridEndOffset = Vec3d.ZERO; }

        double minX = JsonUtils.getDoubleOrDefault(obj, "minX", 0);
        double minY = JsonUtils.getDoubleOrDefault(obj, "minY", 0);
        double minZ = JsonUtils.getDoubleOrDefault(obj, "minZ", 0);
        double maxX = JsonUtils.getDoubleOrDefault(obj, "maxX", 0);
        double maxY = JsonUtils.getDoubleOrDefault(obj, "maxY", 0);
        double maxZ = JsonUtils.getDoubleOrDefault(obj, "maxZ", 0);

        this.setBox(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
    }
}
