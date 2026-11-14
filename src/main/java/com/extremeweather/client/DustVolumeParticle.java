package com.extremeweather.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** One billboard bounding a GPU raymarched density field for a dust devil.
 * This is NOT a replacement for PMWeather's own tornado/cloud renderer. */
public final class DustVolumeParticle extends Particle {
    private static boolean firstRenderLogged;
    private static int renderedSizeMask;
    private final float scale;
    private static final ParticleRenderType VOLUME=new ParticleRenderType() {
        @Override public BufferBuilder begin(Tesselator tesselator,TextureManager textures) {
            if(VolumeShader.vortex()==null)return null;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(VolumeShader::vortex);
            return tesselator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        @Override public boolean isTranslucent(){return true;}
        @Override public String toString(){return "extremeweather:raymarched_dust_devil";}
    };
    private DustVolumeParticle(ClientLevel level,double x,double y,double z,float scale) {
        super(level,x,y,z);
        this.scale=scale;
        this.hasPhysics=false;
        this.lifetime=22;
    }
    public static Particle create(SimpleParticleType type,ClientLevel level,double x,double y,double z,
                                  double dx,double dy,double dz) {
        // count=0 particle packets carry size in x velocity; ordinary /particle
        // diagnostic spawns still default to the medium size.
        float scale=dx>=.55 && dx<=1.65?(float)dx:1f;
        return new DustVolumeParticle(level,x,y,z,scale);
    }
    @Override public ParticleRenderType getRenderType(){return VOLUME;}
    @Override public void render(VertexConsumer vertices,Camera camera,float partialTick) {
        // ParticleEngine may draw behind the "Loading terrain" screen.
        // Only report a gameplay-visible render after that screen is gone.
        if(!firstRenderLogged && net.minecraft.client.Minecraft.getInstance().screen==null) {
            firstRenderLogged=true;
            com.mojang.logging.LogUtils.getLogger().info("ExtremeWeather dust volume rendered with world GUI visible");
        }
        if(net.minecraft.client.Minecraft.getInstance().screen==null) {
            int bit=scale<.8f?1:scale>1.4f?4:2;
            if((renderedSizeMask & bit)==0) {
                renderedSizeMask|=bit;
                com.mojang.logging.LogUtils.getLogger().info("ExtremeWeather dust size rendered: {} scale={}",
                    bit==1?"small":bit==4?"large":"medium",scale);
            }
        }
        Vec3 cameraPos=camera.getPosition();
        Vector3f base=new Vector3f((float)(x-cameraPos.x),(float)(y-cameraPos.y),(float)(z-cameraPos.z));
        Vector3f right=new Vector3f(1,0,0).rotate(camera.rotation()).mul(5.0f*scale);
        // The funnel stands upright in the world. Rotating its vertical axis
        // with camera pitch made it lean toward viewers looking up/down.
        Vector3f up=new Vector3f(0,18.0f*scale,0);
        float alpha=Math.min(1f,(lifetime-age)/2f);
        vertex(vertices,new Vector3f(base).sub(right),0,1,alpha);
        vertex(vertices,new Vector3f(base).add(right),1,1,alpha);
        vertex(vertices,new Vector3f(base).add(right).add(up),1,0,alpha);
        vertex(vertices,new Vector3f(base).sub(right).add(up),0,0,alpha);
    }
    private static void vertex(VertexConsumer sink,Vector3f at,float u,float v,float alpha) {
        sink.addVertex(at.x,at.y,at.z).setUv(u,v).setColor(.53f,.36f,.23f,alpha);
    }
}
