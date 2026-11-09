package com.extremeweather.client;

import com.extremeweather.ExtremeWeather;
import com.extremeweather.particle.EWParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Original particle motion, using PMWeather's installed texture sheet IDs. */
public final class WhirlParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final double driftX, driftZ;
    private WhirlParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,float r,float g,float b) {
        super(level,x,y,z,dx,dy,dz);
        this.sprites=sprites;
        this.driftX=dx;
        this.driftZ=dz;
        this.gravity=0;
        this.friction=.94f;
        this.lifetime=38+level.random.nextInt(20);
        this.quadSize=.7f+level.random.nextFloat()*.65f;
        this.setColor(r,g,b);
        this.setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        super.tick();
        if(!removed) {
            this.xd+=-driftZ*.025;
            this.zd+=driftX*.025;
            this.yd+=.008;
            this.setSpriteFromAge(sprites);
        }
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    private record Provider(SpriteSet sprites,float r,float g,float b) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz) {
            return new WhirlParticle(level,x,y,z,dx,dy,dz,sprites,r,g,b);
        }
    }
    @EventBusSubscriber(modid=ExtremeWeather.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(EWParticles.DUST.get(),sprites->new Provider(sprites,.70f,.55f,.38f));
            event.registerSpecial(EWParticles.DUST_VOLUME.get(),DustVolumeParticle::create);
            event.registerSpriteSet(EWParticles.LAND.get(),sprites->new Provider(sprites,.83f,.79f,.72f));
            event.registerSpriteSet(EWParticles.WATER.get(),sprites->new Provider(sprites,.62f,.82f,1f));
            event.registerSpriteSet(EWParticles.SNOW.get(),sprites->new Provider(sprites,.91f,.97f,1f));
        }
    }
}
