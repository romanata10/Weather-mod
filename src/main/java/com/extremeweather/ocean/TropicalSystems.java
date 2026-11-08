package com.extremeweather.ocean;

import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nullved.pmweatherapi.storm.NearbyStorms;
import java.util.*;

/** Tropical rain bands around an existing PMWeather storm, not a replacement cloud renderer. */
public final class TropicalSystems {
    public enum Basin { HURRICANE, TYPHOON, CYCLONE }
    private final List<System> systems=new ArrayList<>();
    private final List<Pulse> pulses=new ArrayList<>();
    private long ticks;
    public boolean spawn(ServerLevel level,Vec3 center,Basin basin) {
        if(systems.size()>=2 || !level.hasChunkAt(BlockPos.containing(center)))return false;
        systems.add(new System(center,basin,1200));return true;
    }
    public boolean spawnPulse(ServerLevel level,Vec3 center) {
        if(pulses.size()>=2 || !level.hasChunkAt(BlockPos.containing(center)))return false;
        pulses.add(new Pulse(center));return true;
    }
    public void tick(ServerLevel level) {
        ticks++;
        if(ticks%2000==0 && systems.size()<2)attemptBirth(level);
        if(ticks%10!=0)return;
        for(Iterator<Pulse> it=pulses.iterator();it.hasNext();) {
            Pulse wave=it.next();
            if(++wave.age>95){it.remove();continue;}
            wave.tick(level);
        }
        for(Iterator<System> it=systems.iterator();it.hasNext();) {
            System system=it.next();
            if(++system.age>system.life || !level.hasChunkAt(BlockPos.containing(system.center))) {it.remove();continue;}
            Vec3 wind=WindEngine.getWind(BlockPos.containing(system.center),level);
            boolean ocean=level.getBiome(BlockPos.containing(system.center)).is(BiomeTags.IS_OCEAN);
            if(!ocean)system.power=Math.max(.15,system.power-.0025); // decay after landfall
            system.center=system.center.add(wind.x*.6,0,wind.z*.6);
            system.render(level,ticks);
            if(ticks%40==0)system.gust(level,wind);
            // A very rare pressure-driven meteotsunami, not a seismic earthquake.
            if(system.basin==Basin.CYCLONE && system.age%200==0 && pulses.size()<2 && level.random.nextInt(30)==0)
                pulses.add(new Pulse(system.center));
        }
    }
    private void attemptBirth(ServerLevel level) {
        if(level.players().isEmpty())return;
        var player=level.players().get(level.random.nextInt(level.players().size()));
        for(var storm:NearbyStorms.get(level).stormsNearPlayer(player,512)) {
            BlockPos origin=BlockPos.containing(storm.position);
            if(!level.hasChunkAt(origin) || !level.getBiome(origin).is(BiomeTags.IS_OCEAN))continue;
            if(level.getBiome(origin).value().getBaseTemperature()<.7 || level.random.nextFloat()>.35)continue;
            if(systems.stream().anyMatch(s->s.center.distanceToSqr(storm.position)<350*350))continue;
            Basin basin=Basin.values()[level.random.nextInt(Basin.values().length)];
            // Add rain bands around PMWeather's native cyclone rather than inventing
            // a second cloud deck independent of PM's storm engine.
            if(com.extremeweather.compat.PMStormBridge.cyclone(level,storm.position)==null)continue;
            systems.add(new System(storm.position,basin,500+level.random.nextInt(1100)));
            break;
        }
    }
    private static final class Pulse {
        final Vec3 center;int age;
        Pulse(Vec3 center){this.center=center;}
        void tick(ServerLevel level) {
            double radius=8+age*2.0;
            for(var player:level.players()) {
                double distance=Math.hypot(player.getX()-center.x,player.getZ()-center.z);
                if(Math.abs(distance-radius)>12 || !level.hasChunkAt(player.blockPosition()))continue;
                if(!level.getBiome(player.blockPosition()).is(BiomeTags.IS_OCEAN) && player.getY()>level.getSeaLevel()+4)continue;
                double dx=(player.getX()-center.x)/(distance+.01);
                double dz=(player.getZ()-center.z)/(distance+.01);
                for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,player.getBoundingBox().inflate(10))) {
                    if(entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative())continue;
                    entity.push(dx*.25,.12,dz*.25);entity.hurtMarked=true;
                }
                for(int i=0;i<8;i++) {
                    double theta=level.random.nextDouble()*Math.PI*2;
                    int x=(int)Math.floor(player.getX()+Math.cos(theta)*10);
                    int z=(int)Math.floor(player.getZ()+Math.sin(theta)*10);
                    BlockPos crest=new BlockPos(x,level.getSeaLevel()+1,z);
                    if(!level.hasChunkAt(crest) || !level.getFluidState(crest.below()).is(net.minecraft.tags.FluidTags.WATER))continue;
                    level.sendParticles(ParticleTypes.SPLASH,x+.5,level.getSeaLevel()+.8,z+.5,7,2,.5,2,.15);
                    if(level.random.nextInt(4)==0)SurgeData.get(level).place(level,crest,100);
                }
            }
        }
    }
    private static final class System {
        Vec3 center;final Basin basin;final int life;int age;double power=1;
        System(Vec3 center,Basin basin,int life){this.center=center;this.basin=basin;this.life=life;}
        void render(ServerLevel level,long ticks) {
            // Thin spiral arms: PMWeather still draws the actual cloud deck.
            for(int arm=0;arm<4;arm++)for(int sample=0;sample<8;sample++) {
                double r=14+sample*10;
                double angle=arm*Math.PI/2 + sample*.38 + ticks*.005;
                double x=center.x+Math.cos(angle)*r,z=center.z+Math.sin(angle)*r;
                BlockPos at=BlockPos.containing(x,center.y,z);
                if(!level.hasChunkAt(at))continue;
                level.sendParticles(ParticleTypes.RAIN,x,Math.max(level.getSeaLevel()+12,center.y+8),z,2,4,4,4,.03);
                if(sample%3==0)level.sendParticles(ParticleTypes.CLOUD,x,Math.min(level.getMaxBuildHeight()-10,center.y+54),z,1,2,1,2,.01);
            }
        }
        void gust(ServerLevel level,Vec3 wind) {
            for(var player:level.players()) {
                double dist=Math.hypot(player.getX()-center.x,player.getZ()-center.z);
                if(dist>105 || dist<12)continue; // calm eye
                if(!level.hasChunkAt(player.blockPosition()))continue;
                double strength=power*(1-Math.abs(dist-48)/110);
                for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(12))) {
                    if(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative())continue;
                    e.push(wind.x*strength*.18,0,wind.z*strength*.18);e.hurtMarked=true;
                }
                if(level.random.nextInt(4)==0 && level.getBiome(player.blockPosition()).is(BiomeTags.IS_OCEAN)) {
                    BlockPos crest=new BlockPos(player.getBlockX()+level.random.nextInt(20)-10,level.getSeaLevel()+1,player.getBlockZ()+level.random.nextInt(20)-10);
                    SurgeData.get(level).place(level,crest,100+level.random.nextInt(80));
                }
            }
        }
    }
}
