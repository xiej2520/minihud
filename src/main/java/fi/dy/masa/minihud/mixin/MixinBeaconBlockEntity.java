package fi.dy.masa.minihud.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import fi.dy.masa.minihud.renderer.OverlayRendererBeaconRange;

@Mixin(BeaconBlockEntity.class)
public abstract class MixinBeaconBlockEntity extends BlockEntity
{
    @Shadow
    private int level;

    @Shadow
    private List<BeaconBlockEntity.BeamSegment> beamSegments;

    @Unique
    private boolean hadBeamPre;

    @Unique
    private int levelPre = -1;

    public MixinBeaconBlockEntity(BlockEntityType<?> type) {
        super(type);
    }


    @Inject(method = "markRemoved", at = @At("RETURN"))
    private void minihud_onRemoved(CallbackInfo ci)
    {
        OverlayRendererBeaconRange.INSTANCE.onBlockStatusChange(this.getPos());
    }

    @Inject(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/BeaconBlockEntity;updateLevel(III)V"
            ))
    private void minihud_onUpdateSegmentsPre(CallbackInfo ci)
    {
        if (this.levelPre != -1) {
            this.levelPre = this.level;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void minihud_onUpdateSegmentsPost(CallbackInfo ci)
    {
        boolean hasBeam = this.beamSegments.isEmpty() == false;

        if (this.levelPre != this.level || this.hadBeamPre != hasBeam)
        {
            OverlayRendererBeaconRange.INSTANCE.onBlockStatusChange(pos);
            this.levelPre = this.level;
            this.hadBeamPre = hasBeam;
        }
    }
}
