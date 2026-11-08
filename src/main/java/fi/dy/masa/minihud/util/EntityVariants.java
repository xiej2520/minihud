package fi.dy.masa.minihud.util;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;

public class EntityVariants
{
    // HorseEntity#initTextureInfo(): variant = color | (marking << 8)
    private static final String[] HORSE_COLORS = { "white", "creamy", "chestnut", "brown", "black", "gray", "dark_brown" };
    private static final String[] HORSE_MARKINGS = { "none", "white", "white_field", "white_dots", "black_dots" };
    private static final String[] CAT_TYPES = { "tabby", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "white", "jellie", "all_black" };
    private static final String[] PARROT_VARIANTS = { "red_blue", "blue", "green", "yellow_blue", "grey" };
    private static final String[] LLAMA_VARIANTS = { "creamy", "white", "brown", "gray" };
    private static final String[] RABBIT_TYPES = { "brown", "white", "black", "black_and_white", "gold", "salt_and_pepper" };

    public static String getHorseColor(int variant)
    {
        return HORSE_COLORS[(variant & 0xFF) % HORSE_COLORS.length];
    }

    public static String getHorseMarking(int variant)
    {
        return HORSE_MARKINGS[((variant & 0xFF00) >> 8) % HORSE_MARKINGS.length];
    }

    public static String getRabbitType(int type)
    {
        if (type == 99)
        {
            return "the_killer_bunny";
        }

        return type >= 0 && type < RABBIT_TYPES.length ? RABBIT_TYPES[type] : "unknown (" + type + ")";
    }

    private static String getName(String[] names, int index)
    {
        return index >= 0 && index < names.length ? names[index] : "unknown (" + index + ")";
    }

    @Nullable
    public static String getVariantLine(Entity entity)
    {
        String a = GuiBase.TXT_AQUA;
        String r = GuiBase.TXT_RST;

        if (entity instanceof HorseEntity)
        {
            int variant = ((HorseEntity) entity).getVariant();
            return String.format("Horse Variant: %s%s%s / %s%s%s", a, getHorseColor(variant), r, GuiBase.TXT_LIGHT_PURPLE, getHorseMarking(variant), r);
        }
        else if (entity instanceof CatEntity)
        {
            CatEntity cat = (CatEntity) entity;
            String collar = cat.isTamed() ? " (" + cat.getCollarColor().getName() + " collar)" : "";
            return String.format("Cat Variant: %s%s%s%s", a, getName(CAT_TYPES, cat.getCatType()), r, collar);
        }
        else if (entity instanceof FoxEntity)
        {
            return String.format("Fox Variant: %s%s%s", a, ((FoxEntity) entity).getFoxType().getKey(), r);
        }
        else if (entity instanceof ParrotEntity)
        {
            return String.format("Parrot Variant: %s%s%s", a, getName(PARROT_VARIANTS, ((ParrotEntity) entity).getVariant()), r);
        }
        else if (entity instanceof LlamaEntity)
        {
            LlamaEntity llama = (LlamaEntity) entity;
            return String.format("Llama Variant: %s%s%s, Strength: %s%d%s", a, getName(LLAMA_VARIANTS, llama.getVariant()), r,
                GuiBase.TXT_LIGHT_PURPLE, llama.getStrength(), r);
        }
        else if (entity instanceof RabbitEntity)
        {
            return String.format("Rabbit Variant: %s%s%s", a, getRabbitType(((RabbitEntity) entity).getRabbitType()), r);
        }
        else if (entity instanceof MooshroomEntity)
        {
            return String.format("Mooshroom Variant: %s%s%s", a, ((MooshroomEntity) entity).getMooshroomType().name().toLowerCase(), r);
        }
        else if (entity instanceof SheepEntity)
        {
            return String.format("Sheep Color: %s%s%s", a, ((SheepEntity) entity).getColor().getName(), r);
        }
        else if (entity instanceof TropicalFishEntity)
        {
            int variant = ((TropicalFishEntity) entity).getVariant();
            return String.format("Tropical Fish Variant: %s%s%s [%s%s%s/%s%s%s]", a,
                StringUtils.translate(TropicalFishEntity.getTranslationKey(variant)), r,
                GuiBase.TXT_YELLOW, TropicalFishEntity.getBaseDyeColor(variant).getName(), r,
                GuiBase.TXT_LIGHT_PURPLE, TropicalFishEntity.getPatternDyeColor(variant).getName(), r);
        }

        return null;
    }
}
