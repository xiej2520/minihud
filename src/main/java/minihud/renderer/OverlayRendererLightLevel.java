package minihud.renderer;

import java.util.ArrayList;
import java.util.List;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.BlockUtils;
import minihud.util.value.LightLevelRenderCondition;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.chunk.Chunk;

import malilib.config.option.ColorConfig;
import malilib.config.option.Vec2dConfig;
import malilib.render.buffer.VertexBuilder;
import malilib.render.overlay.VboRenderObject;
import malilib.util.data.Color4f;
import malilib.util.data.Identifier;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RenderWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.Direction;
import malilib.util.position.Vec3d;
import minihud.Reference;
import minihud.config.Configs;
import minihud.config.RendererToggle;
import minihud.util.value.LightLevelMarkerMode;
import minihud.util.value.LightLevelNumberMode;

public class OverlayRendererLightLevel extends MiniHudOverlayRenderer
{
    private static final Identifier NUMBER_TEXTURE = new Identifier(Reference.MOD_ID, "textures/misc/light_level_numbers.png");

    private final List<LightLevelInfo> lightInfoList = new ArrayList<>();
    private Direction lastDirection = Direction.NORTH;

    public OverlayRendererLightLevel()
    {
        super(COLORED_TEXTURED_QUADS_BUILDER, COLORED_LINES_BUILDER);
    }

    @Override
    public boolean shouldRender()
    {
        return RendererToggle.LIGHT_LEVEL.isRendererEnabled();
    }

    @Override
    public boolean needsUpdate(Entity entity)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
               Math.abs(EntityWrap.getX(entity) - this.lastUpdatePos.getX()) > 4 ||
               Math.abs(EntityWrap.getY(entity) - this.lastUpdatePos.getY()) > 4 ||
               Math.abs(EntityWrap.getZ(entity) - this.lastUpdatePos.getZ()) > 4 ||
               (Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() &&
                   this.lastDirection != EntityWrap.getClosestHorizontalLookingDirection(entity));
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        this.startBuffers();

        BlockPos pos = EntityWrap.getEntityBlockPos(entity);
        //long pre = System.nanoTime();
        this.updateLightLevels(GameWrap.getClientWorld(), pos);
        //System.out.printf("LL markers: %d, time: %.3f s\n", LIGHT_INFOS.size(), (double) (System.nanoTime() - pre) / 1000000000D);
        this.renderLightLevels(cameraPos);

        this.uploadBuffers();
        this.lastDirection = EntityWrap.getClosestHorizontalLookingDirection(entity);
        this.needsUpdate = false;
    }

    @Override
    protected void preRender()
    {
        super.preRender();

        RenderWrap.bindTexture(NUMBER_TEXTURE);
    }

    @Override
    public void allocateGlResources()
    {
        this.quadRenderer = this.allocateBuffer(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR, VboRenderObject::setupArrayPointersPosUvColor);
        this.outlineRenderer = this.allocateBuffer(GL11.GL_LINES);
    }

    protected void renderLightLevels(Vec3d cameraPos)
    {
        final int count = this.lightInfoList.size();
        Entity entity = GameWrap.getCameraEntity();

        if (count > 0)
        {
            Direction numberFacing = Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() ? EntityWrap.getClosestHorizontalLookingDirection(entity) : Direction.NORTH;
            LightLevelNumberMode numberMode = Configs.Generic.LIGHT_LEVEL_NUMBER_MODE.getValue();
            LightLevelMarkerMode markerMode = Configs.Generic.LIGHT_LEVEL_MARKER_MODE.getValue();
            boolean useColoredNumbers = Configs.Generic.LIGHT_LEVEL_COLORED_NUMBERS.getBooleanValue();
            int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
            int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();

            if (numberMode == LightLevelNumberMode.BLOCK || numberMode == LightLevelNumberMode.BOTH)
            {
                this.renderNumbers(cameraPos, LightLevelNumberMode.BLOCK,
                                   Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_BLOCK,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_LIT,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_DIM,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_DARK,
                                   useColoredNumbers, safeThreshold, dimThreshold, numberFacing);
            }

            if (numberMode == LightLevelNumberMode.SKY || numberMode == LightLevelNumberMode.BOTH)
            {
                this.renderNumbers(cameraPos, LightLevelNumberMode.SKY,
                                   Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_SKY,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_LIT,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_DIM,
                                   Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_DARK,
                                   useColoredNumbers, safeThreshold, dimThreshold, numberFacing);
            }

            if (markerMode == LightLevelMarkerMode.SQUARE)
            {
                this.renderMarkers(this::renderLightLevelSquare, cameraPos, safeThreshold, dimThreshold);
            }
            else if (markerMode == LightLevelMarkerMode.CROSS)
            {
                this.renderMarkers(this::renderLightLevelCross, cameraPos, safeThreshold, dimThreshold);
            }
        }
    }

    protected void renderNumbers(Vec3d cameraPos,
                                 LightLevelNumberMode mode,
                                 Vec2dConfig cfgOff,
                                 ColorConfig cfgColorLit,
                                 ColorConfig cfgColorDim,
                                 ColorConfig cfgColorDark,
                                 boolean useColoredNumbers,
                                 int safeThreshold,
                                 int dimThreshold,
                                 Direction numberFacing)
    {
        double ox = cfgOff.getValue().x;
        double oz = cfgOff.getValue().y;
        double tmpX, tmpZ;
        Color4f colorLit, colorDim, colorDark;
        double offsetY = Configs.Generic.LIGHT_LEVEL_RENDER_OFFSET.getDoubleValue();

        switch (numberFacing)
        {
            case SOUTH: tmpX =  ox; tmpZ =  oz; break;
            case WEST:  tmpX = -oz; tmpZ =  ox; break;
            case EAST:  tmpX =  oz; tmpZ = -ox; break;
            case NORTH:
            default:    tmpX = -ox; tmpZ = -oz;
        }

        if (useColoredNumbers)
        {
            colorLit = cfgColorLit.getColor();
            colorDim = cfgColorDim.getColor();
            colorDark = cfgColorDark.getColor();
        }
        else
        {
            colorLit = Color4f.WHITE;
            colorDim = colorLit;
            colorDark = colorLit;
        }

        this.renderLightLevelNumbers(tmpX + cameraPos.x, cameraPos.y - offsetY, tmpZ + cameraPos.z,
                                     numberFacing, safeThreshold, dimThreshold, mode, colorLit, colorDim, colorDark);
    }

    protected void renderMarkers(IMarkerRenderer renderer, Vec3d cameraPos, int safeThreshold, int dimThreshold)
    {
        Color4f colorBlockLit = Configs.Colors.LIGHT_LEVEL_MARKER_BLOCK_LIT.getColor();
        Color4f colorDim = Configs.Colors.LIGHT_LEVEL_MARKER_DIM.getColor();
        Color4f colorSkyLit = Configs.Colors.LIGHT_LEVEL_MARKER_SKY_LIT.getColor();
        Color4f colorDark = Configs.Colors.LIGHT_LEVEL_MARKER_DARK.getColor();
        LightLevelRenderCondition condition = Configs.Generic.LIGHT_LEVEL_MARKER_CONDITION.getValue();
        double markerSize = Configs.Generic.LIGHT_LEVEL_MARKER_SIZE.getDoubleValue();

        double offsetX = cameraPos.x;
        double offsetY = cameraPos.y - Configs.Generic.LIGHT_LEVEL_RENDER_OFFSET.getDoubleValue();
        double offsetZ = cameraPos.z;
        double offset1 = (1.0 - markerSize) / 2.0;
        double offset2 = (1.0 - offset1);
        boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        Color4f color;
        VertexBuilder lineBuilder = this.lineBuilder;

        for (LightLevelInfo info : this.lightInfoList)
        {
            if (condition.shouldRender(info.block, dimThreshold, safeThreshold))
            {
                BlockPos pos = BlockPos.fromPacked(info.packedPos);
                double x = pos.getX() - offsetX;
                double y = (autoHeight ? info.y : pos.getY()) - offsetY;
                double z = pos.getZ() - offsetZ;

                if (info.block < safeThreshold)
                {
                    color = info.sky >= safeThreshold ? colorSkyLit : colorDark;
                }
                else if (info.block > dimThreshold)
                {
                    color = colorBlockLit;
                }
                else
                {
                    color = colorDim;
                }
                renderer.render(x, y, z, color, offset1, offset2, lineBuilder);
            }
        }
    }

    protected void renderLightLevelNumbers(double dx, double dy, double dz,
                                           Direction facing,
                                           int safeThreshold,
                                           int dimThreshold,
                                           LightLevelNumberMode numberMode,
                                           Color4f colorLit,
                                           Color4f colorDim,
                                           Color4f colorDark)
    {
        LightLevelRenderCondition condition = Configs.Generic.LIGHT_LEVEL_NUMBER_CONDITION.getValue();
        boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        Color4f color;

        for (LightLevelInfo info : this.lightInfoList)
        {
            if (condition.shouldRender(info.block, dimThreshold, safeThreshold)) {
                BlockPos pos = BlockPos.fromPacked(info.packedPos);
                double x = pos.getX() - dx;
                double y = (autoHeight ? info.y : pos.getY()) - dy;
                double z = pos.getZ() - dz;

                int lightLevel = numberMode == LightLevelNumberMode.BLOCK ? info.block : info.sky;

                if (lightLevel < safeThreshold)
                {
                    color = colorDark;
                }
                else if (lightLevel > dimThreshold)
                {
                    color = colorLit;
                }
                else
                {
                    color = colorDim;
                }
                this.renderLightLevelTextureColor(x, y, z, facing, lightLevel, color, this.quadBuilder);
            }
        }
    }

    protected void renderLightLevelTextureColor(double x, double y, double z, Direction facing,
                                                int lightLevel, Color4f color, VertexBuilder builder)
    {
        float w = 0.25f;
        float u = (lightLevel & 0x3) * w;
        float v = (lightLevel >> 2) * w;

        switch (facing)
        {
            case NORTH:
                builder.posUvColor(x    , y, z    , u    , v    , color);
                builder.posUvColor(x    , y, z + 1, u    , v + w, color);
                builder.posUvColor(x + 1, y, z + 1, u + w, v + w, color);
                builder.posUvColor(x + 1, y, z    , u + w, v    , color);
                break;

            case SOUTH:
                builder.posUvColor(x + 1, y, z + 1, u    , v    , color);
                builder.posUvColor(x + 1, y, z    , u    , v + w, color);
                builder.posUvColor(x    , y, z    , u + w, v + w, color);
                builder.posUvColor(x    , y, z + 1, u + w, v    , color);
                break;

            case EAST:
                builder.posUvColor(x + 1, y, z    , u    , v    , color);
                builder.posUvColor(x    , y, z    , u    , v + w, color);
                builder.posUvColor(x    , y, z + 1, u + w, v + w, color);
                builder.posUvColor(x + 1, y, z + 1, u + w, v    , color);
                break;

            case WEST:
                builder.posUvColor(x    , y, z + 1, u    , v    , color);
                builder.posUvColor(x + 1, y, z + 1, u    , v + w, color);
                builder.posUvColor(x + 1, y, z    , u + w, v + w, color);
                builder.posUvColor(x    , y, z    , u + w, v    , color);
                break;

            default:
        }
    }

    protected void renderLightLevelCross(double x, double y, double z, Color4f color,
                                         double offset1, double offset2, VertexBuilder builder)
    {
        builder.posColor(x + offset1, y, z + offset1, color);
        builder.posColor(x + offset2, y, z + offset2, color);

        builder.posColor(x + offset1, y, z + offset2, color);
        builder.posColor(x + offset2, y, z + offset1, color);
    }

    private void renderLightLevelSquare(double x, double y, double z, Color4f color,
                                        double offset1, double offset2, VertexBuilder builder)
    {
        builder.posColor(x + offset1, y, z + offset1, color);
        builder.posColor(x + offset1, y, z + offset2, color);

        builder.posColor(x + offset1, y, z + offset2, color);
        builder.posColor(x + offset2, y, z + offset2, color);

        builder.posColor(x + offset2, y, z + offset2, color);
        builder.posColor(x + offset2, y, z + offset1, color);

        builder.posColor(x + offset2, y, z + offset1, color);
        builder.posColor(x + offset1, y, z + offset1, color);
    }

    private void updateLightLevels(World world, BlockPos center)
    {
        this.lightInfoList.clear();

        int radius = Configs.Generic.LIGHT_LEVEL_RANGE.getIntegerValue();
        final int minX = center.getX() - radius;
        final int minY = center.getY() - radius;
        final int minZ = center.getZ() - radius;
        final int maxX = center.getX() + radius;
        final int maxY = center.getY() + radius;
        final int maxZ = center.getZ() + radius;
        final int minCX = (minX >> 4);
        final int minCZ = (minZ >> 4);
        final int maxCX = (maxX >> 4);
        final int maxCZ = (maxZ >> 4);

        BlockPos.MutBlockPos mutablePos = new BlockPos.MutBlockPos();

        final int worldTopHeight = world.getHeight();
        final boolean collisionCheck = Configs.Generic.LIGHT_LEVEL_COLLISION_CHECK.getBooleanValue();
        final boolean underWater = Configs.Generic.LIGHT_LEVEL_UNDER_WATER.getBooleanValue();
        final boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        final boolean skipBlockCheck = Configs.Generic.LIGHT_LEVEL_SKIP_BLOCK_CHECK.getBooleanValue();

        for (int cx = minCX; cx <= maxCX; ++cx)
        {
            final int startX = Math.max( cx << 4      , minX);
            final int endX   = Math.min((cx << 4) + 15, maxX);

            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                final int startZ = Math.max( cz << 4      , minZ);
                final int endZ   = Math.min((cz << 4) + 15, maxZ);
                Chunk chunk = world.getChunk(cx, cz);
                final int startY = Math.max(minY, 0); // TODO: world.getMinY()?
                final int endY   = Math.min(maxY, chunk.getTopFilledSegment() + 15 + 1);

                for (int y = startY; y <= endY; ++y)
                {
                    if (y > startY)
                    {
                        // If there are no blocks in the section below this layer, then we can skip it
                        ExtendedBlockStorage section = chunk.getBlockStorageArray()[(y - 1) >> 4];
                        if (section == null || section.isEmpty())
                        {
                            continue;
                        }
                    }

                    for (int x = startX; x <= endX; ++x)
                    {
                        for (int z = startZ; z <= endZ; ++z)
                        {
                            if (this.canSpawnAtWrapper(x, y, z, chunk, world, skipBlockCheck) == false)
                            {
                                continue;
                            }

                            mutablePos.set(x, y, z);
                            IBlockState state = chunk.getBlockState(mutablePos);

                            if (collisionCheck)
                            {
                                AxisAlignedBB bb = state.getCollisionBoundingBox(world, mutablePos);
                                // check if hitbox contains the top center of the block below: (0.5, 0.5), and y is nonempty (snow layer 1)
                                if (bb != null && (bb.minX < 0.5 && bb.maxX > 0.5 && bb.minY != bb.maxY && bb.minZ < 0.5 && bb.maxZ > 0.5))
                                {
                                    continue;
                                }
                            }
                            if (underWater == false && BlockUtils.isFluidBlock(state))
                            {
                                continue;
                            }

                            int block = y < worldTopHeight ? chunk.getLightFor(EnumSkyBlock.BLOCK, mutablePos) : 0;
                            int sky   = y < worldTopHeight ? chunk.getLightFor(EnumSkyBlock.SKY, mutablePos) : 15;
                            // air blocks have full bounding box until 1.13
                            double topY = (state.getBlock() == Blocks.AIR || BlockUtils.isFluidBlock(state)) ? 0.0 : state.getBoundingBox(world, mutablePos).maxY;

                            // Don't render the light level marker if it would be raised all the way to the next block space
                            if (autoHeight == false || topY < 1)
                            {
                                float posY = topY >= 0 ? y + (float) topY : y;
                                this.lightInfoList.add(new LightLevelInfo(mutablePos.toPackedLong(), posY, block, sky));
                                //y += 2; // if the spot is spawnable, that means the next spawnable spot can be the third block up
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canSpawnAtWrapper(int x, int y, int z, Chunk chunk, World world, boolean skipBlockCheck)
    {
        try
        {
            return canSpawnAt(x, y, z, chunk, world, skipBlockCheck);
        }
        catch (Exception e)
        {
            MessageDispatcher.warning("This dimension seems to have missing block tag data, the light level will not use the normal block spawnability checks in this dimension. This is known to happen on some Waterfall/BungeeCord/ViaVersion/whatever setups that have an older MC version at the back end.");
            //tagsBroken = true;

            return false;
        }
    }

    /**
     * This method mimics the one from WorldEntitySpawner, but takes in the Chunk to avoid that lookup
     */
    public static boolean canSpawnAt(int x, int y, int z, Chunk chunk, World world, boolean skipBlockCheck)
    {
        BlockPos.MutBlockPos pos = new BlockPos.MutBlockPos(x, y - 1, z); // TODO: where to allocate
        IBlockState stateDown = chunk.getBlockState(pos);
        if (skipBlockCheck)
        {
            if (stateDown.getBlock().equals(Blocks.AIR) || BlockUtils.isFluidBlock(stateDown))
            {
                return false;
            }
        }
        else
        {
            if (stateDown.isTopSolid() == false || stateDown.getBlock() == Blocks.BEDROCK || stateDown.getBlock() == Blocks.BARRIER)
            {
                return false;
            }
        }
        pos.set(x, y, z);
        IBlockState state = chunk.getBlockState(pos);

        pos.set(x, y + 1, z);
        IBlockState stateUp = chunk.getBlockState(pos);

        if (state.getMaterial() == Material.WATER)
        {
            pos.set(x, y + 2, z);
            IBlockState stateUp2 = chunk.getBlockState(pos);

            return stateUp.getMaterial() == Material.WATER &&
                   stateUp2.isNormalCube() == false;
        }

        return WorldEntitySpawner.isValidEmptySpawnBlock(state) &&
               WorldEntitySpawner.isValidEmptySpawnBlock(stateUp);
    }

    public static class LightLevelInfo
    {
        public final long packedPos;
        public final byte block;
        public final byte sky;
        public final float y;

        public LightLevelInfo(long packedPos, float y, int block, int sky)
        {
            this.packedPos = packedPos;
            this.block = (byte) block;
            this.sky = (byte) sky;
            this.y = y;
        }
    }

    private interface IMarkerRenderer
    {
        void render(double x, double y, double z, Color4f color, double offset1, double offset2, VertexBuilder builder);
    }
}
