package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.mob.ZombieVillagerEntity;

@Mixin(ZombieVillagerEntity.class)
public interface IMixinZombieVillagerEntity
{
    @Accessor("conversionTimer")
    int minihud_getConversionTimer();
}
