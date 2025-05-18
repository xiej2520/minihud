package fi.dy.masa.minihud.util;

import java.util.HashMap;
import java.util.Locale;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.StructureFeature;
import fi.dy.masa.minihud.config.StructureToggle;

public enum StructureType
{
    BURIED_TREASURE     (StructureToggle.OVERLAY_STRUCTURE_BURIED_TREASURE,     "buried_treasure",      DimensionType.getId(DimensionType.OVERWORLD)),
    DESERT_PYRAMID      (StructureToggle.OVERLAY_STRUCTURE_DESERT_PYRAMID,      "desert_pyramid",       DimensionType.getId(DimensionType.OVERWORLD)),
    IGLOO               (StructureToggle.OVERLAY_STRUCTURE_IGLOO,               "igloo",                DimensionType.getId(DimensionType.OVERWORLD)),
    JUNGLE_TEMPLE       (StructureToggle.OVERLAY_STRUCTURE_JUNGLE_TEMPLE,       "jungle_pyramid",       DimensionType.getId(DimensionType.OVERWORLD)),
    MANSION             (StructureToggle.OVERLAY_STRUCTURE_MANSION,             "mansion",              DimensionType.getId(DimensionType.OVERWORLD)),
    MINESHAFT           (StructureToggle.OVERLAY_STRUCTURE_MINESHAFT,           "mineshaft",            DimensionType.getId(DimensionType.OVERWORLD)),
    OCEAN_MONUMENT      (StructureToggle.OVERLAY_STRUCTURE_OCEAN_MONUMENT,      "monument",             DimensionType.getId(DimensionType.OVERWORLD)),
    OCEAN_RUIN          (StructureToggle.OVERLAY_STRUCTURE_OCEAN_RUIN,          "ocean_ruin",           DimensionType.getId(DimensionType.OVERWORLD)),
    PILLAGER_OUTPOST    (StructureToggle.OVERLAY_STRUCTURE_PILLAGER_OUTPOST,    "pillager_outpost",     DimensionType.getId(DimensionType.OVERWORLD)),
    SHIPWRECK           (StructureToggle.OVERLAY_STRUCTURE_SHIPWRECK,           "shipwreck",            DimensionType.getId(DimensionType.OVERWORLD)),
    STRONGHOLD          (StructureToggle.OVERLAY_STRUCTURE_STRONGHOLD,          "stronghold",           DimensionType.getId(DimensionType.OVERWORLD)),
    VILLAGE             (StructureToggle.OVERLAY_STRUCTURE_VILLAGE,             "village",              DimensionType.getId(DimensionType.OVERWORLD)),
    WITCH_HUT           (StructureToggle.OVERLAY_STRUCTURE_WITCH_HUT,           "swamp_hut",            DimensionType.getId(DimensionType.OVERWORLD)),

    RUINED_PORTAL       (StructureToggle.OVERLAY_STRUCTURE_RUINED_PORTAL,       "ruined_portal",        DimensionType.getId(DimensionType.OVERWORLD), DimensionType.getId(DimensionType.THE_NETHER)),

    BASTION_REMNANT     (StructureToggle.OVERLAY_STRUCTURE_BASTION_REMNANT,     "bastion_remnant",      DimensionType.getId(DimensionType.THE_NETHER)),
    NETHER_FOSSIL       (StructureToggle.OVERLAY_STRUCTURE_NETHER_FOSSIL,       "nether_fossil",        DimensionType.getId(DimensionType.THE_NETHER)),
    NETHER_FORTRESS     (StructureToggle.OVERLAY_STRUCTURE_NETHER_FORTRESS,     "fortress",             DimensionType.getId(DimensionType.THE_NETHER)),

    END_CITY            (StructureToggle.OVERLAY_STRUCTURE_END_CITY,            "endcity",              DimensionType.getId(DimensionType.THE_END));

    public static final ImmutableList<StructureType> VALUES;
    private static final HashMap<String, StructureType> ID_TO_TYPE = new HashMap<>();

    static
    {
        VALUES = ImmutableList.copyOf(values());

        for (StructureType type : VALUES)
        {
            if (type.feature != null)
            {
                Identifier key = Registry.STRUCTURE_FEATURE.getId(type.feature);

                if (key != null)
                {
                    ID_TO_TYPE.put(key.toString(), type);
                }
            }
        }
    }

    @Nullable
    public static StructureType byStructureId(String id)
    {
        return ID_TO_TYPE.get(id);
    }

    private final StructureToggle toggle;
    private final String structureName;
    private final StructureFeature<?> feature;
    private final ImmutableSet<Identifier> dims;

    StructureType(StructureToggle toggle, String structureName, Identifier... dims)
    {
        this.toggle = toggle;
        this.structureName = structureName;
        this.feature = StructureFeature.STRUCTURES.get(structureName.toLowerCase(Locale.ROOT));
        this.dims = ImmutableSet.copyOf(dims);
    }

    public boolean existsInDimension(DimensionType dimId)
    {
        return this.dims.contains(DimensionType.getId(dimId)); // a bit of a meh... but works in vanilla
    }

    public String getStructureName()
    {
        return this.structureName;
    }

    public StructureFeature<?> getFeature()
    {
        return this.feature;
    }

    public StructureToggle getToggle()
    {
        return this.toggle;
    }

    public boolean isEnabled()
    {
        return this.toggle.getToggleOption().getBooleanValue();
    }
}
