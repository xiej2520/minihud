package fi.dy.masa.minihud.mixin;

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

    @Unique
    private int levelPre;

    public MixinBeaconBlockEntity(BlockEntityType<?> type) {
        super(type);
    }


    @Inject(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/BeaconBlockEntity;updateLevel(III)V"
            ))
    private void onUpdateSegmentsPre(CallbackInfo ci)
    {
        this.levelPre = this.level;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onUpdateSegmentsPost(CallbackInfo ci)
    {
        if (this.levelPre != this.level)
        {
            OverlayRendererBeaconRange.setNeedsUpdate();
        }
    }
}
