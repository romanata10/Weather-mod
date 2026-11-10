package com.extremeweather.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Red fullbright sprite tendril, visible at night without whitening the red
 * branches with an end-rod or borrowing PMWeather's private shaders. */
final class SpriteGlowParticle extends TextureSheetParticle {
    SpriteGlowParticle(ClientLevel level,double x,double y,double z,SpriteSet sprites) {
        super(level,x,y,z,0,0,0);
        hasPhysics=false;
        gravity=0;
        xd=yd=zd=0;
        lifetime=18;
        quadSize=2.2f;
        setColor(1f,.055f,.085f);
        setSpriteFromAge(sprites);
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getLightColor(float partialTick){return 0xF000F0;}
}
