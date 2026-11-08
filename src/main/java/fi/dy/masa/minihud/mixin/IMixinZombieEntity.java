package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.mob.ZombieEntity;

@Mixin(ZombieEntity.class)
public interface IMixinZombieEntity
{
    @Accessor("inWaterTime")
    int minihud_getInWaterTime();

    @Accessor("ticksUntilWaterConversion")
    int minihud_getTicksUntilWaterConversion();
}
