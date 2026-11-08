package com.extremeweather.weather;

import net.minecraft.network.chat.Component;

/**
 * Family of a vortex. Distinguishes what the funnel is made of and what it does.
 */
public enum VortexType {

    /** Classic land tornado, EF0 - EF5. */
    TORNADO(true, false, false, false, 1.0F),
    /** Weak tornado that forms under a growing cumulus - narrow and dusty. */
    LANDSPOUT(true, false, false, false, 0.55F),
    /** Tornado over water. Spray filled, weak but can move inland. */
    WATERSPOUT(true, true, false, false, 0.55F),
    /** Dust devil: fair weather whirl on a hot day. No rain, no cloud. */
    DUST_DEVIL(true, false, true, false, 0.20F),
    /** Gustnado: short lived spin on a gust front, no connection to the cloud base. */
    GUSTNADO(true, false, false, false, 0.25F),
    /** Fire whirl born inside a wildfire, full of flame and burning debris. */
    FIRE_WHIRL(true, false, false, true, 0.60F),
    /** Snow / ice whirl over a frozen landscape. */
    SNOW_WHIRL(true, false, false, false, 0.35F),
    /** Hyper vortex - a multi-vortex giant that only forms in EF5 supercells. */
    HYPERVORTEX(true, false, false, false, 1.5F);

    public final boolean damaging;
    public final boolean water;
    public final boolean fairWeather;
    public final boolean fire;
    /** Multiplier applied to wind, damage and block destruction. */
    public final float power;

    VortexType(boolean damaging, boolean water, boolean fairWeather, boolean fire, float power) {
        this.damaging = damaging;
        this.water = water;
        this.fairWeather = fairWeather;
        this.fire = fire;
        this.power = power;
    }

    public boolean isSmall() {
        return this == DUST_DEVIL || this == GUSTNADO || this == LANDSPOUT || this == SNOW_WHIRL;
    }

    public Component displayName() {
        return Component.translatable("extremeweather.vortex." + name().toLowerCase());
    }

    public static VortexType byName(String name) {
        for (VortexType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
