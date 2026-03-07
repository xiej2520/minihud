package fi.dy.masa.minihud.renderer;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;

import fi.dy.masa.minihud.config.Configs;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.shapes.ShapeChunkTicket;
import fi.dy.masa.minihud.util.DataStorage;

public class OverlayRendererSimulationDistance extends OverlayRendererBase implements AutoCloseable
{
    public static final OverlayRendererSimulationDistance INSTANCE = new OverlayRendererSimulationDistance(RendererToggle.OVERLAY_SIMULATION_DISTANCE);
    protected final RendererToggle toggle;
    protected final ShapeChunkTicket shape;

    protected OverlayRendererSimulationDistance(RendererToggle toggle)
    {
        this.toggle = toggle;
        this.shape = new ShapeChunkTicket();
        this.useCulling = false;
    }

    @Override
    public String getName()
    {
        return "SimulationDistance";
    }

    public void setNeedsUpdate()
    {
        this.shape.setNeedsUpdate();
    }

    public void setPos(@NotNull BlockPos pos)
    {
        this.shape.setPos(pos);
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return this.toggle.getBooleanValue() && DataStorage.getInstance().isSimulationDistanceKnown();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        return this.shape.needsUpdate(entity, mc);
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        this.shape.setBlockTickingEnabled(Configs.Generic.SIMULATION_DISTANCE_BLOCK_TICKING_OVERLAY_ENABLED.getBooleanValue());
        this.shape.setOuterEnabled(Configs.Generic.SIMULATION_DISTANCE_OUTER_OVERLAY_ENABLED.getBooleanValue());
        this.shape.setRadius(DataStorage.getInstance().getSimulationDistance());
        this.shape.update(cameraPos, entity, mc, profiler);
    }

    @Override
    public boolean hasData()
    {
        return this.shape.hasData();
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.shape.render(cameraPos, mc, profiler);
    }

    @Override
    public void draw(Vec3 cameraPos)
    {
        this.shape.draw(cameraPos);
    }

    @Override
    public void reset()
    {
        super.reset();
        this.shape.reset();
    }

    @Override
    public void close()
    {
        this.reset();
    }

    @Nullable
    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("shape", this.shape.toJson());

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        if (obj.has("shape"))
        {
            ShapeChunkTicket shape = new ShapeChunkTicket();
            shape.fromJson(obj.getAsJsonObject("shape"));
        }
    }
}
