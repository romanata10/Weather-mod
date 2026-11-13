package com.extremeweather.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Visible spray along the bounded wave impulse; never represents a fluid block. */
final class WaveCrestParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private static boolean firstRenderLogged;
    WaveCrestParticle(ClientLevel level,double x,double y,double z,SpriteSet sprites) {
        super(level,x,y,z,0,0,0);
        this.sprites=sprites;
        hasPhysics=false;
        gravity=0;
        xd=zd=0;yd=.008;
        lifetime=18;
        quadSize=2.7f;
        setColor(.83f,.93f,1f);
        setAlpha(.82f);
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        super.tick();
        if(!removed) {
            setAlpha(.82f*(lifetime-age)/(float)lifetime);
            setSpriteFromAge(sprites);
        }
    }
    @Override public void render(VertexConsumer vertices,Camera camera,float partialTick) {
        if(!firstRenderLogged && net.minecraft.client.Minecraft.getInstance().screen==null) {
            firstRenderLogged=true;
            com.mojang.logging.LogUtils.getLogger().info("ExtremeWeather wave crest sprite rendered in joined client");
        }
        super.render(vertices,camera,partialTick);
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getLightColor(float partialTick){return 0xF000F0;}
}
