package minihud.util.value;

import com.google.common.collect.ImmutableList;
import malilib.config.value.BaseOptionListConfigValue;

public class LightLevelRenderCondition extends BaseOptionListConfigValue
{
    public static final LightLevelRenderCondition ALWAYS      = new LightLevelRenderCondition("always",      "minihud.name.light_level_render_condition.always",    (l, t) -> true);
    public static final LightLevelRenderCondition NEVER       = new LightLevelRenderCondition("never",       "minihud.name.light_level_render_condition.never",     (l, t) -> false);
    public static final LightLevelRenderCondition SAFE        = new LightLevelRenderCondition("safe",        "minihud.name.light_level_render_condition.safe",      (l, t) -> l >= t);
    public static final LightLevelRenderCondition SPAWNABLE   = new LightLevelRenderCondition("spawnable",   "minihud.name.light_level_render_condition.spawnable", (l, t) -> l < t);

    public static final ImmutableList<LightLevelRenderCondition> VALUES = ImmutableList.of(ALWAYS, NEVER, SAFE, SPAWNABLE);

    private final Condition condition;

    private LightLevelRenderCondition(String name, String translationKey, Condition condition)
    {
        super(name, translationKey);
        this.condition = condition;
    }


    public boolean shouldRender(int lightLevel, int safeThreshold)
    {
        return this.condition.shouldRender(lightLevel, safeThreshold);
    }

    private interface Condition
    {
        boolean shouldRender(int lightLevel, int safeThreshold);
    }
}
