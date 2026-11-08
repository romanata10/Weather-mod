package com.extremeweather.disaster;

import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.nullved.pmweatherapi.storm.NearbyStorms;

import java.util.*;

/**
 * Add-on phenomena derived from PMWeather's authoritative storm positions and wind.
 * All damage, movement and state changes happen only on the server; particles are
 * broadcast by ServerLevel so every nearby multiplayer client sees the same event.
 */
public final class DisasterEngine {
    private final List<Whirl> whirls=new ArrayList<>();
    private final Map<UUID,Long> lastStorm=new HashMap<>();
    private long ticks;

    public void tick(ServerLevel level) {
        ticks++;
        if(ticks%5==0) {
            for(Iterator<Whirl> it=whirls.iterator();it.hasNext();) {
                Whirl w=it.next();
                if(w.age++>w.life || !level.hasChunkAt(BlockPos.containing(w.pos))) {it.remove();continue;}
                w.tick(level,ticks);
            }
        }
        if(ticks%40!=0)return;
        for(ServerPlayer player:level.players()) {
            if(!level.hasChunkAt(player.blockPosition()))continue;
            var storms=NearbyStorms.get(level).stormsNearPlayer(player,512);
            double distance=Double.POSITIVE_INFINITY;
            for(var storm:storms)distance=Math.min(distance,Math.hypot(storm.position.x-player.getX(),storm.position.z-player.getZ()));
            boolean stormy=distance<180;
            Vec3 wind=WindEngine.getWind(player.blockPosition(),level);
            double windSpeed=wind.horizontalDistance();
            if(stormy) {
                if(level.random.nextInt(7)==0)hail(level,player);
                if(level.random.nextInt(60)==0)specialLightning(level,player);
                if(windSpeed>.45 && level.random.nextInt(20)==0)gust(level,player,wind);
                if(level.random.nextInt(45)==0)tryWhirl(level,player,true);
            } else if(level.random.nextInt(75)==0)tryWhirl(level,player,false);
            if(level.getFluidState(player.blockPosition()).is(FluidTags.WATER) && windSpeed>.3)
                player.push(wind.x*.06,Math.sin(ticks*.07+player.getX()*.25)*Math.min(.04,windSpeed*.03),wind.z*.06);
            if(!stormy && lastStorm.containsKey(player.getUUID()) && ticks-lastStorm.get(player.getUUID())<1800 && level.isDay() && !level.isRaining())
                rainbow(level,player);
            if(stormy)lastStorm.put(player.getUUID(),ticks);
        }
        lastStorm.entrySet().removeIf(e->ticks-e.getValue()>1800);
    }
    private void hail(ServerLevel level,ServerPlayer player) {
        if(!level.canSeeSky(player.blockPosition()))return;
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL,player.getX(),player.getY()+12,player.getZ(),35,12,4,12,.06);
        if(level.random.nextInt(3)==0 && !player.isCreative())player.hurt(level.damageSources().generic(),1.0f);
    }
    private void specialLightning(ServerLevel level,ServerPlayer player) {
        BlockPos pos=player.blockPosition().offset(level.random.nextInt(48)-24,0,level.random.nextInt(48)-24);
        if(!level.hasChunkAt(pos))return;
        pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos);
        if(level.random.nextInt(8)==0) {
            // Superbolt: exceptionally powerful cloud-to-ground strike.
            LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(level);
            if(bolt!=null){bolt.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(bolt);}
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,pos.getX(),pos.getY()+12,pos.getZ(),100,5,12,5,.15);
        } else if(level.random.nextInt(4)==0) {
            // Upper-atmosphere red sprite: visual only, no ground damage.
            level.sendParticles(ParticleTypes.END_ROD,pos.getX(),Math.min(level.getMaxBuildHeight()-18,pos.getY()+110),pos.getZ(),60,12,16,12,.03);
        } else {
            // Rare ball lightning moves through the air and lightly shocks entities.
            Vec3 center=pos.getCenter().add(0,4,0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,center.x,center.y,center.z,45,1.5,1.5,1.5,.04);
            for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(3)))
                entity.hurt(level.damageSources().lightningBolt(),2.0f);
        }
    }
    private void gust(ServerLevel level,ServerPlayer player,Vec3 wind) {
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(15))) {
            if(entity instanceof ServerPlayer p && p.isCreative())continue;
            entity.push(wind.x*.35,.04,wind.z*.35);
            entity.hurtMarked=true;
        }
        level.sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY()+2,player.getZ(),30,10,2,10,.08);
    }
    private void tryWhirl(ServerLevel level,ServerPlayer player,boolean stormy) {
        if(whirls.size()>10)return;
        BlockPos pos=player.blockPosition().offset(level.random.nextInt(100)-50,0,level.random.nextInt(100)-50);
        if(!level.hasChunkAt(pos))return;
        pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos);
        Holder<Biome> biome=level.getBiome(pos);
        double temp=biome.value().getBaseTemperature();
        Kind kind;
        if(stormy)kind=level.getFluidState(pos.below()).is(FluidTags.WATER)?Kind.WATERSPOUT:Kind.LANDSPOUT;
        else if(!level.isRaining() && temp>1.0 && !biome.is(BiomeTags.IS_OCEAN))kind=Kind.DUST_DEVIL;
        else if(temp<.15)kind=Kind.SNOW_WHIRL;
        else return;
        for(Whirl whirl:whirls)if(whirl.pos.distanceToSqr(pos.getCenter())<40*40)return;
        whirls.add(new Whirl(pos.getCenter(),kind,300+level.random.nextInt(300)));
    }
    private void rainbow(ServerLevel level,ServerPlayer player) {
        if(level.random.nextInt(3)!=0)return;
        for(int i=0;i<7;i++) {
            double theta=(i+level.random.nextDouble())/7*Math.PI;
            double x=player.getX()+Math.cos(theta)*32;
            double y=player.getY()+6+Math.sin(theta)*22-i*.4;
            level.sendParticles(ParticleTypes.END_ROD,x,y,player.getZ()+45,2,.5,.3,.5,0);
        }
    }
    private enum Kind { DUST_DEVIL, WATERSPOUT, LANDSPOUT, SNOW_WHIRL }
    private static final class Whirl {
        Vec3 pos;final Kind kind;final int life;int age;
        Whirl(Vec3 pos,Kind kind,int life){this.pos=pos;this.kind=kind;this.life=life;}
        void tick(ServerLevel level,long clock) {
            Vec3 wind=WindEngine.getWind(BlockPos.containing(pos),level);
            pos=pos.add(wind.x*.5,0,wind.z*.5);
            for(int i=0;i<14;i++) {
                double h=level.random.nextDouble()*16,phase=clock*.25+h*.5+i*2.4;
                double r=.8+h*.1;
                var particle=kind==Kind.WATERSPOUT?ParticleTypes.SPLASH:kind==Kind.SNOW_WHIRL?ParticleTypes.SNOWFLAKE:ParticleTypes.CLOUD;
                level.sendParticles(particle,pos.x+Math.cos(phase)*r,pos.y+h,pos.z+Math.sin(phase)*r,1,0,.1,0,.02);
            }
            if(clock%20==0)for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos,pos.add(0,15,0)).inflate(3))) {
                if(entity instanceof ServerPlayer player && player.isCreative())continue;
                double dx=entity.getX()-pos.x,dz=entity.getZ()-pos.z;
                entity.push(-dz*.035,.12,dx*.035);entity.hurtMarked=true;
            }
        }
    }
}
