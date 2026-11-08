package com.extremeweather.weather;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Silhouette of a funnel. The radius profile of a vortex is fully described by
 * {@link #radiusAt(float, float, float)} so the client renderer and the server physics
 * always agree on how wide the vortex is at a given height.
 */
public enum VortexShape {

    /** Thin, sinuous, snake like funnel. Typical of decaying or weak tornadoes. */
    ROPE(1.0F, 0.5F, 0F),
    /** Classic cone: wide at the cloud base, narrow at the ground. */
    CONE(1.0F, 0.18F, 0F),
    /** Almost constant width, straight tube. */
    STOVE_PIPE(1.0F, 0.75F, 0F),
    /** Fat tube that widens towards the ground. */
    BARREL(1.0F, 1.35F, 0F),
    /** Wedge: wider than it is tall, the classic violent EF4/EF5 look. */
    WEDGE(1.0F, 2.1F, 0F),
    /** Several suction vortices orbiting inside one large funnel. */
    MULTI_VORTEX(1.0F, 1.7F, 0F),
    /** Tornado that leans and staggers like a drunk - extremely photogenic and deadly. */
    DEAD_MAN_WALKING(1.0F, 1.4F, 1F),
    /** El Reno style record monster: multiple kilometres wide, almost a wall of dirt. */
    EL_RENO(1.0F, 3.4F, 0.3F);

    public final float heightScale;
    /** Width at the ground relative to the nominal width. */
    public final float groundWidthFactor;
    /** How much the funnel leans / sways. */
    public final float lean;

    VortexShape(float heightScale, float groundWidthFactor, float lean) {
        this.heightScale = heightScale;
        this.groundWidthFactor = groundWidthFactor;
        this.lean = lean;
    }

    public Component displayName() {
        return Component.translatable("extremeweather.shape." + name().toLowerCase());
    }

    /**
     * Radius of the funnel at normalised height {@code t} (0 = ground, 1 = cloud base).
     *
     * @param width nominal (cloud base) half width in blocks
     * @param ef    EF rating, drives how massive the funnel looks
     */
    public float radiusAt(float t, float width, float ef) {
        float ground = Math.max(1.0F, width * groundWidthFactor);
        return switch (this) {
            case ROPE -> {
                float wobble = 1.0F + 0.35F * Mth.sin(t * 9.0F);
                yield Mth.lerp(t, ground, width * 0.45F) * wobble;
            }
            case CONE -> Mth.lerp(t, ground * 0.35F, width);
            case STOVE_PIPE -> Mth.lerp(t, ground * 0.85F, width);
            case BARREL -> width * (0.85F + 0.35F * Mth.sin(t * Mth.PI));
            case WEDGE, EL_RENO -> Mth.lerp(t, width * groundWidthFactor, width * 0.75F);
            case MULTI_VORTEX -> Mth.lerp(t, width * 1.1F, width * 0.9F);
            case DEAD_MAN_WALKING -> Mth.lerp(t, width * 1.25F, width * 0.85F) * (1.0F + 0.18F * Mth.sin(t * 5.0F));
        };
    }

    public static VortexShape byName(String name) {
        for (VortexShape shape : values()) if (shape.name().equalsIgnoreCase(name)) return shape;
        return null;
    }

    /** Picks a silhouette according to the EF rating, with rare shapes gated behind high EF. */
    public static VortexShape pick(RandomSource random, float ef, VortexType type) {
        if (type.isSmall()) {
            return random.nextBoolean() ? ROPE : CONE;
        }
        if (type == VortexType.FIRE_WHIRL || type == VortexType.SNOW_WHIRL) {
            return ef > 2.0F && random.nextFloat() < 0.3F ? WEDGE : (random.nextBoolean() ? ROPE : CONE);
        }
        if (ef >= 4.4F) {
            float roll = random.nextFloat();
            if (roll < 0.022F) {
                return EL_RENO;
            }
            if (roll < 0.070F) {
                return DEAD_MAN_WALKING;
            }
            if (roll < 0.30F) {
                return MULTI_VORTEX;
            }
            return roll < 0.72F ? WEDGE : BARREL;
        }
        if (ef >= 3.0F) {
            float roll = random.nextFloat();
            if (roll < 0.040F) {
                return DEAD_MAN_WALKING;
            }
            if (roll < 0.16F) {
                return MULTI_VORTEX;
            }
            return roll < 0.62F ? WEDGE : STOVE_PIPE;
        }
        if (ef >= 1.5F) {
            return random.nextFloat() < 0.25F ? MULTI_VORTEX : STOVE_PIPE;
        }
        return random.nextFloat() < 0.55F ? ROPE : CONE;
    }
}
