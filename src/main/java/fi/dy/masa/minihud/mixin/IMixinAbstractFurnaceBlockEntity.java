package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface IMixinAbstractFurnaceBlockEntity
{
    @Accessor("recipesUsed")
    Map<Identifier, Integer> minihud_getUsedRecipes();
}
