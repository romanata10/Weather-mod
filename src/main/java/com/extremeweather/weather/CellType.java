package com.extremeweather.weather;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Kind of a parent storm cell. Determines the radius, lifetime, tornado risk and
 * which extra phenomena (hail, surge, spiral bands) the cell can produce.
 */
public enum CellType {

    /** Ordinary single cell thunderstorm. Short lived, weak wind, small tornado chance. */
    THUNDERSTORM(0.55F, 1.0F, 0.35F, false, false, false, 2400),
    /** Cluster of cells, wider rain shield, several weak funnels possible. */
    MULTI_CELL(0.70F, 1.6F, 0.55F, true, false, false, 3600),
    /** Rotating supercell - the tornado factory. Produces giant hail and long track tornadoes. */
    SUPERCELL(1.00F, 2.4F, 0.95F, true, false, false, 6000),
    /** Weak tropical system over warm water. Precursor of a hurricane. */
    TROPICAL_STORM(0.80F, 2.8F, 0.30F, false, true, false, 8000),
    /** Hurricane: spiral bands, eyewall, storm surge, flooding, waterspouts. */
    HURRICANE(1.00F, 4.2F, 0.45F, false, true, true, 16000),
    /** Pacific hurricane. Same physics, slightly larger. */
    TYPHOON(1.00F, 4.6F, 0.45F, false, true, true, 18000),
    /** Indian ocean / southern hemisphere cyclone. Biggest of the family. */
    CYCLONE(1.00F, 4.8F, 0.45F, false, true, true, 18000),
    /** Frontal system over cold water. Lots of wind, big waves, few tornadoes. */
    EXTRATROPICAL(0.80F, 3.0F, 0.25F, true, false, false, 7000);

    public final float baseIntensity;
    public final float radiusScale;
    /** Probability multiplier applied to the configured tornado chance. */
    public final float tornadoRisk;
    public final boolean rotating;
    public final boolean tropical;
    public final boolean hurricaneForce;
    public final int baseLifetime;

    CellType(float baseIntensity, float radiusScale, float tornadoRisk, boolean rotating,
             boolean tropical, boolean hurricaneForce, int baseLifetime) {
        this.baseIntensity = baseIntensity;
        this.radiusScale = radiusScale;
        this.tornadoRisk = tornadoRisk;
        this.rotating = rotating;
        this.tropical = tropical;
        this.hurricaneForce = hurricaneForce;
        this.baseLifetime = baseLifetime;
    }

    public Component displayName() {
        return Component.translatable("extremeweather.cell." + name().toLowerCase());
    }

    public static CellType byName(String name) {
        for (CellType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    /** Land cell: hot climates favour supercells, cold ones favour squall lines. */
    public static CellType pickLand(RandomSource random, boolean cold) {
        float roll = random.nextFloat();
        if (cold) {
            return roll < 0.55F ? MULTI_CELL : EXTRATROPICAL;
        }
        if (roll < 0.42F) {
            return THUNDERSTORM;
        }
        if (roll < 0.72F) {
            return MULTI_CELL;
        }
        return SUPERCELL;
    }

    /** Ocean cell: the warmer the water, the higher the chance of a tropical cyclone. */
    public static CellType pickOcean(RandomSource random, float warmWaterFactor, boolean enabled) {
        if (!enabled) {
            return random.nextBoolean() ? EXTRATROPICAL : MULTI_CELL;
        }
        float roll = random.nextFloat() * warmWaterFactor;
        if (roll > 0.86F) {
            return CYCLONE;
        }
        if (roll > 0.70F) {
            return TYPHOON;
        }
        if (roll > 0.46F) {
            return HURRICANE;
        }
        if (roll > 0.22F) {
            return TROPICAL_STORM;
        }
        return EXTRATROPICAL;
    }
}
