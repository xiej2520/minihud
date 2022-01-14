package minihud.util.value;

import com.google.common.collect.ImmutableList;
import malilib.config.value.BaseOptionListConfigValue;

public class LightLevelRenderCondition extends BaseOptionListConfigValue
{
    public static final LightLevelRenderCondition ALWAYS      = new LightLevelRenderCondition("always",      "minihud.name.light_level_render_condition.always",    (b, d, s) -> true);
    public static final LightLevelRenderCondition NEVER       = new LightLevelRenderCondition("never",       "minihud.name.light_level_render_condition.never",     (b, d, s) -> false);
    public static final LightLevelRenderCondition SAFE        = new LightLevelRenderCondition("safe",        "minihud.name.light_level_render_condition.safe",      (b, d, s) -> b >= s && (d <= s || b > d));
    public static final LightLevelRenderCondition DIM         = new LightLevelRenderCondition("dim",         "minihud.name.light_level_render_condition.dim",       (b, d, s) -> b <= d && d > s);
    public static final LightLevelRenderCondition SPAWNABLE   = new LightLevelRenderCondition("spawnable",   "minihud.name.light_level_render_condition.spawnable", (b, d, s) -> b < s);

    public static final ImmutableList<LightLevelRenderCondition> VALUES = ImmutableList.of(ALWAYS, NEVER, SAFE, DIM, SPAWNABLE);

    private final Condition condition;

    private LightLevelRenderCondition(String name, String translationKey, Condition condition)
    {
        super(name, translationKey);
        this.condition = condition;
    }


    public boolean shouldRender(int blockLightLevel, int dimThreshold, int safeThreshold)
    {
        return this.condition.shouldRender(blockLightLevel, dimThreshold, safeThreshold);
    }

    private interface Condition
    {
        boolean shouldRender(int blockLightLevel, int dimThreshold, int safeThreshold);
    }
}
