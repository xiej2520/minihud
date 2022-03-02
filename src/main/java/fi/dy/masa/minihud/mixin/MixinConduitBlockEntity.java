package fi.dy.masa.minihud.mixin;

import java.util.List;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.OverlayRendererConduitRange;
import fi.dy.masa.minihud.util.ConduitExtra;

@Mixin(ConduitBlockEntity.class)
public abstract class MixinConduitBlockEntity extends BlockEntity implements ConduitExtra
{
    @Shadow @Final private List<BlockPos> activatingBlocks;
    private int minihud_activatingBlockCount;

    public MixinConduitBlockEntity(BlockEntityType<?> type) {
        super(type);
    }

    @Override
    public int getCurrentActivatingBlockCount()
    {
        return this.activatingBlocks.size();
    }

    @Override
    public int getStoredActivatingBlockCount()
    {
        return this.minihud_activatingBlockCount;
    }

    @Override
    public void setActivatingBlockCount(int count)
    {
        this.minihud_activatingBlockCount = count;
    }

    @Inject(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/ConduitBlockEntity;setActive(Z)V"))
    private void minihud_postActiveBlockScan(CallbackInfo ci)
    {
        if (RendererToggle.OVERLAY_CONDUIT_RANGE.getBooleanValue())
        {
            int count = this.getCurrentActivatingBlockCount();
            int countBefore = this.getStoredActivatingBlockCount();

            if (count != countBefore)
            {
                OverlayRendererConduitRange.INSTANCE.onBlockStatusChange(this.pos);
                this.setActivatingBlockCount(count);
            }
        }
    }
}
