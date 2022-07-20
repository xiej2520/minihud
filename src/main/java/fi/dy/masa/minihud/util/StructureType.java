package fi.dy.masa.minihud.util;

import java.util.HashMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import fi.dy.masa.minihud.config.StructureToggle;

public enum StructureType
{
    BURIED_TREASURE     (StructureToggle.OVERLAY_STRUCTURE_BURIED_TREASURE,     "Buried_Treasure"),
    DESERT_PYRAMID      (StructureToggle.OVERLAY_STRUCTURE_DESERT_PYRAMID,      "Desert_Pyramid"),
    IGLOO               (StructureToggle.OVERLAY_STRUCTURE_IGLOO,               "Igloo"),
    JUNGLE_TEMPLE       (StructureToggle.OVERLAY_STRUCTURE_JUNGLE_TEMPLE,       "Jungle_Pyramid"),
    MANSION             (StructureToggle.OVERLAY_STRUCTURE_MANSION,             "Mansion"),
    MINESHAFT           (StructureToggle.OVERLAY_STRUCTURE_MINESHAFT,           "Mineshaft"),
    OCEAN_MONUMENT      (StructureToggle.OVERLAY_STRUCTURE_OCEAN_MONUMENT,      "Monument"),
    OCEAN_RUIN          (StructureToggle.OVERLAY_STRUCTURE_OCEAN_RUIN,          "Ocean_Ruin"),
    PILLAGER_OUTPOST    (StructureToggle.OVERLAY_STRUCTURE_PILLAGER_OUTPOST,    "Pillager_Outpost"),
    SHIPWRECK           (StructureToggle.OVERLAY_STRUCTURE_SHIPWRECK,           "Shipwreck"),
    STRONGHOLD          (StructureToggle.OVERLAY_STRUCTURE_STRONGHOLD,          "Stronghold"),
    VILLAGE             (StructureToggle.OVERLAY_STRUCTURE_VILLAGE,             "Village"),
    WITCH_HUT           (StructureToggle.OVERLAY_STRUCTURE_WITCH_HUT,           "Swamp_Hut"),

    NETHER_FORTRESS     (StructureToggle.OVERLAY_STRUCTURE_NETHER_FORTRESS,     "Fortress"),

    END_CITY            (StructureToggle.OVERLAY_STRUCTURE_END_CITY,            "EndCity"),

    UNKNOWN             (StructureToggle.OVERLAY_STRUCTURE_UNKNOWN);

    private static final HashMap<String, StructureType> STRUCTURE_ID_TO_TYPE = new HashMap<>();
    public static final ImmutableList<StructureType> VALUES = ImmutableList.copyOf(values());

    public static StructureType fromStructureId(String id)
    {
        if (STRUCTURE_ID_TO_TYPE.isEmpty())
        {
            VALUES.forEach(st -> st.structureIds.forEach(i -> STRUCTURE_ID_TO_TYPE.put(i, st)));
        }

        return STRUCTURE_ID_TO_TYPE.getOrDefault(id, UNKNOWN);
    }

    private final StructureToggle toggle;
    private final ImmutableSet<String> structureIds;

    StructureType(StructureToggle toggle, String... structuresIds)
    {
        this.toggle = toggle;
        this.structureIds = ImmutableSet.copyOf(structuresIds);
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
