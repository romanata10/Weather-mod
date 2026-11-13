package com.extremeweather.particle;

import com.extremeweather.ExtremeWeather;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/** The particles use PMWeather texture resource IDs at runtime, without shipping copies. */
public final class EWParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(Registries.PARTICLE_TYPE,ExtremeWeather.MODID);
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> SPRITE_GLOW=PARTICLES.register("sprite_glow",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> DUST=PARTICLES.register("dust_whirl",()->new SimpleParticleType(false));
    // One shader-drawn volumetric quad per dust devil, not an atlas sprite list.
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> DUST_VOLUME=PARTICLES.register("dust_volume",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> LAND=PARTICLES.register("landspout",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> WATER=PARTICLES.register("waterspout",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> WAVE_CREST=PARTICLES.register("wave_crest",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> SNOW=PARTICLES.register("snow_whirl",()->new SimpleParticleType(false));
    private EWParticles() {}
}
