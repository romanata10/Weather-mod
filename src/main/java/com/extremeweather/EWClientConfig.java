package com.extremeweather;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client only rendering options. Anything that costs frames per second is controllable here.
 */
public final class EWClientConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue RENDER_CLOUDS;
    public static final ForgeConfigSpec.BooleanValue HIDE_VANILLA_CLOUDS;
    public static final ForgeConfigSpec.IntValue CLOUD_DETAIL;
    public static final ForgeConfigSpec.BooleanValue RENDER_FUNNELS;
    public static final ForgeConfigSpec.IntValue FUNNEL_SEGMENTS;
    public static final ForgeConfigSpec.BooleanValue RENDER_WAVES;
    public static final ForgeConfigSpec.BooleanValue RENDER_RAINBOW;
    public static final ForgeConfigSpec.BooleanValue RENDER_SPRITES;
    public static final ForgeConfigSpec.BooleanValue RENDER_SMOKE_PLUMES;
    public static final ForgeConfigSpec.BooleanValue WEATHER_FOG;
    public static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE;
    public static final ForgeConfigSpec.BooleanValue RENDER_PRECIPITATION_STREAKS;
    public static final ForgeConfigSpec.DoubleValue FOG_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SHAKE_STRENGTH;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Cloud rendering").push("clouds");
        RENDER_CLOUDS = b.comment("Draw the mod's volumetric style cloud layer.").define("renderClouds", true);
        HIDE_VANILLA_CLOUDS = b.comment("Temporarily hide vanilla clouds while a simulated storm is nearby.")
                .define("hideVanillaClouds", false);
        CLOUD_DETAIL = b.comment("Cloud puff count multiplier. 1 = low, 3 = cinematic.").defineInRange("cloudDetail", 2, 1, 4);
        b.pop();

        b.comment("Tornado and funnel rendering").push("funnels");
        RENDER_FUNNELS = b.define("renderFunnels", true);
        FUNNEL_SEGMENTS = b.comment("Radial segments per funnel ring (16 is a good balance).")
                .defineInRange("funnelSegments", 16, 8, 48);
        RENDER_SMOKE_PLUMES = b.define("renderSmokePlumes", true);
        b.pop();

        b.comment("Water, sky and atmosphere").push("atmosphere");
        RENDER_WAVES = b.comment("Draw the storm wave surface over oceans.").define("renderWaves", true);
        RENDER_RAINBOW = b.define("renderRainbow", true);
        RENDER_SPRITES = b.comment("Draw red sprites above supercells.").define("renderSprites", true);
        RENDER_PRECIPITATION_STREAKS = b.comment("Extra slanted rain / hail streaks inside storms.")
                .define("renderPrecipitationStreaks", true);
        WEATHER_FOG = b.comment("Thicken fog inside hurricanes and smoke curtains.").define("weatherFog", true);
        FOG_STRENGTH = b.defineInRange("fogStrength", 1.0D, 0.0D, 3.0D);
        b.pop();

        b.comment("Camera").push("camera");
        CAMERA_SHAKE = b.comment("Shake the camera near tornadoes and thunder.").define("cameraShake", true);
        SHAKE_STRENGTH = b.defineInRange("shakeStrength", 1.0D, 0.0D, 3.0D);
        b.pop();

        SPEC = b.build();
    }

    private EWClientConfig() {
    }
}
