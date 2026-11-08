package com.extremeweather.util;

import com.extremeweather.ExtremeWeather;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Custom damage types. The JSON definitions live in {@code data/extremeweather/damage_type/}.
 */
public final class EWDamageTypes {

    public static final ResourceKey<DamageType> TORNADO = key("tornado");
    public static final ResourceKey<DamageType> DEBRIS = key("debris");
    public static final ResourceKey<DamageType> HAIL = key("hail");
    public static final ResourceKey<DamageType> WILDFIRE = key("wildfire");
    public static final ResourceKey<DamageType> TSUNAMI = key("tsunami");
    public static final ResourceKey<DamageType> BALL_LIGHTNING = key("ball_lightning");
    public static final ResourceKey<DamageType> HYPER_LIGHTNING = key("hyper_lightning");
    public static final ResourceKey<DamageType> DUST_STORM = key("dust_storm");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ExtremeWeather.MODID, name));
    }

    public static DamageSource of(ServerLevel level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    private EWDamageTypes() {
    }
}
