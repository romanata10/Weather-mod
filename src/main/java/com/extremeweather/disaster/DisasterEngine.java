package com.extremeweather.disaster;

import dev.protomanly.pmweather.weather.WindEngine;
import dev.protomanly.pmweather.particle.ModParticleTypes;
import com.extremeweather.particle.EWParticles;
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
    private final List<HailField> hailFields=new ArrayList<>();
    private final List<SquallField> squalls=new ArrayList<>();
    private final List<Whirlpool> whirlpools=new ArrayList<>();
    private final SkyPhenomena sky=new SkyPhenomena();
    private final com.extremeweather.vortex.ManualVortices manual=new com.extremeweather.vortex.ManualVortices();
    private final com.extremeweather.ocean.TropicalSystems tropical=new com.extremeweather.ocean.TropicalSystems();
    private final Map<UUID,Long> lastStorm=new HashMap<>();
    private long ticks;

    public void tick(ServerLevel level) {
        ticks++;
        manual.tick(level);
        tropical.tick(level);
        if(ticks%5==0)for(Iterator<HailField> it=hailFields.iterator();it.hasNext();) {
            HailField field=it.next();
            if(++field.age>80 || !level.hasChunkAt(BlockPos.containing(field.center)))it.remove();
            else field.tick(level);
        }
        if(ticks%20==0)com.extremeweather.ocean.SurgeData.get(level).tick(level);
        if(ticks%10==0)for(Iterator<SquallField> it=squalls.iterator();it.hasNext();) {
            SquallField squall=it.next();
            if(++squall.age>240 || squall.storm.dead || !level.hasChunkAt(BlockPos.containing(squall.storm.position)))
                it.remove();
            else squall.tick(level);
        }
        if(ticks%5==0)for(Iterator<Whirlpool> it=whirlpools.iterator();it.hasNext();) {
            Whirlpool vortex=it.next();
            if(++vortex.age>240 || !level.hasChunkAt(BlockPos.containing(vortex.center)) ||
                    !level.getFluidState(BlockPos.containing(vortex.center).below()).is(FluidTags.WATER))it.remove();
            else vortex.tick(level);
        }
        if(ticks%2==0)sky.tick(level);
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
            if(stormy && level.getBiome(player.blockPosition()).is(BiomeTags.IS_OCEAN)) {
                seaEffects(level,player,wind,windSpeed);
                if(whirlpools.size()<4 && level.random.nextInt(140)==0) {
                    BlockPos water=player.blockPosition().offset(level.random.nextInt(48)-24,0,level.random.nextInt(48)-24);
                    BlockPos surface=new BlockPos(water.getX(),level.getSeaLevel()+1,water.getZ());
                    if(level.hasChunkAt(surface) && level.getFluidState(surface.below()).is(FluidTags.WATER)
                            && whirlpools.stream().noneMatch(w->w.center.distanceToSqr(surface.getCenter())<28*28))
                        whirlpools.add(new Whirlpool(surface.getCenter()));
                }
                if(windSpeed>.8 && level.random.nextInt(12)==0)stormSurge(level,player,wind);
            }
            if(!stormy && lastStorm.containsKey(player.getUUID()) && ticks-lastStorm.get(player.getUUID())<1800 && level.isDay() && !level.isRaining())
                rainbow(level,player);
            if(stormy)lastStorm.put(player.getUUID(),ticks);
        }
        lastStorm.entrySet().removeIf(e->ticks-e.getValue()>1800);
    }
    private void stormSurge(ServerLevel level,ServerPlayer player,Vec3 wind) {
        // Shallow storm surge follows the PMWeather wind direction. Source blocks
        // are saved and removed when the crest recedes, including after a restart.
        int sea=level.getSeaLevel();
        var data=com.extremeweather.ocean.SurgeData.get(level);
        for(int i=0;i<5;i++) {
            int x=player.getBlockX()+level.random.nextInt(30)-15;
            int z=player.getBlockZ()+level.random.nextInt(30)-15;
            BlockPos base=new BlockPos(x,sea,z);
            if(!level.hasChunkAt(base) || !level.getBiome(base).is(BiomeTags.IS_OCEAN))continue;
            BlockPos crest=base.above();
            if(level.getFluidState(base).is(FluidTags.WATER)) {
                data.place(level,crest,140+level.random.nextInt(100));
                level.sendParticles(ModParticleTypes.FOAM.get(),x+.5,sea+1,z+.5,10,1,.6,1,.15);
            }
        }
    }
    public boolean summonFunnel(ServerLevel level,Vec3 pos,com.extremeweather.vortex.FunnelShape shape,int ef) {
        return manual.spawn(level,pos,shape,ef);
    }
    public void clearManual(){manual.clear();}
    public void clearAll(){manual.clear();whirls.clear();whirlpools.clear();hailFields.clear();squalls.clear();sky.clear();tropical.clear();lastStorm.clear();}
    public int activeSqualls(){return squalls.size();}
    public record EffectResult(boolean started,String reason) {}
    /** Operator-triggered effects: return failure when their actual prerequisites are absent. */
    public EffectResult trigger(ServerLevel level,Vec3 position,String effect) {
        BlockPos pos=BlockPos.containing(position);
        if(!level.hasChunkAt(pos))return new EffectResult(false,"The target chunk is not loaded");
        switch(effect) {
            case "hail" -> {
                if(hailFields.size()>=4)return new EffectResult(false,"Hail limit reached (4 per dimension)");
                hailFields.add(new HailField(position));
            }
            case "whirlpool" -> {
                if(whirlpools.size()>=6)return new EffectResult(false,"Whirlpool limit reached (6 per dimension)");
                if(!level.getFluidState(pos.below()).is(FluidTags.WATER))
                    return new EffectResult(false,"A whirlpool requires water below the target position");
                whirlpools.add(new Whirlpool(position));
            }
            case "rainbow" -> sky.rainbow(level,position);
            case "sprite" -> {
                if(!sky.sprite(level,pos))return new EffectResult(false,"Sprite requires night and a nearby PMWeather thundercloud (dayTime="+(level.getDayTime()%24000)+", registered storms="+NearbyStorms.get(level).stormsNearBlock(pos,2048).size()+")");
            }
            case "ball_lightning" -> {
                if(!sky.canSpawnBall())return new EffectResult(false,"Ball lightning limit reached (8 per dimension)");
                sky.spawnBall(level,position.add(0,4,0));
            }
            case "superbolt" -> {
                LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(level);
                if(bolt==null)return new EffectResult(false,"Lightning entity creation failed");
                bolt.moveTo(position.x,position.y,position.z);level.addFreshEntity(bolt);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,position.x,position.y+12,position.z,90,6,12,6,.08);
            }
            case "squall" -> {
                if(squalls.size()>=3)return new EffectResult(false,"Active squall limit reached (3 per dimension)");
                var storm=com.extremeweather.compat.PMStormBridge.spawn(level,
                    dev.protomanly.pmweather.weather.storms.StormTypes.SQUALL,position,.7f,1);
                if(storm==null)return new EffectResult(false,"PMWeather cannot start a squall here or storm limit reached");
                squalls.add(new SquallField(storm));
            }
            case "dust_devil", "waterspout", "landspout", "snow_whirl" -> {
                if(whirls.size()>=12)return new EffectResult(false,"Small vortex limit reached (12 per dimension)");
                if(effect.equals("waterspout") && !level.getFluidState(pos.below()).is(FluidTags.WATER))
                    return new EffectResult(false,"A waterspout requires water below the target position");
                Kind kind=switch(effect) {
                    case "dust_devil" -> Kind.DUST_DEVIL;
                    case "waterspout" -> Kind.WATERSPOUT;
                    case "snow_whirl" -> Kind.SNOW_WHIRL;
                    default -> Kind.LANDSPOUT;
                };
                whirls.add(new Whirl(position,kind,360));
            }
            case "hurricane", "typhoon", "cyclone" -> {
                if(com.extremeweather.compat.PMStormBridge.cyclone(level,position)==null)
                    return new EffectResult(false,"PMWeather cannot start a cyclone here or storm limit reached");
                var basin=switch(effect) {
                    case "typhoon" -> com.extremeweather.ocean.TropicalSystems.Basin.TYPHOON;
                    case "cyclone" -> com.extremeweather.ocean.TropicalSystems.Basin.CYCLONE;
                    default -> com.extremeweather.ocean.TropicalSystems.Basin.HURRICANE;
                };
                tropical.spawn(level,position,basin);
            }
            case "fire_whirl" -> {
                if(com.extremeweather.compat.PMStormBridge.fireWhirl(level,position)==null)
                    return new EffectResult(false,"PMWeather cannot start a fire whirl here or storm limit reached");
            }
            case "meteotsunami", "tsunami" -> {
                if(!level.getBiome(pos).is(BiomeTags.IS_OCEAN))
                    return new EffectResult(false,"A meteotsunami must start over an ocean biome");
                if(!tropical.spawnPulse(level,position))return new EffectResult(false,"Wave limit reached or chunk unloaded");
            }
            case "surge", "flood" -> {
                if(!com.extremeweather.ocean.SurgeData.get(level).place(level,new BlockPos(pos.getX(),level.getSeaLevel()+1,pos.getZ()),200))
                    return new EffectResult(false,"Flood crest needs empty space above water at sea level");
            }
            default -> {return new EffectResult(false,"Unknown disaster");}
        }
        return new EffectResult(true,"Started "+effect+" at "+pos.toShortString());
    }
    /** Real PMWeather squall cloud, coupled to server-authoritative gusts.
     * The PM storm remains the only cloud, so no decorative particle cloud is added. */
    private static final class SquallField {
        final dev.protomanly.pmweather.weather.Storm storm;int age;
        SquallField(dev.protomanly.pmweather.weather.Storm storm){this.storm=storm;}
        void tick(ServerLevel level) {
            Vec3 center=storm.position;
            for(ServerPlayer viewer:level.players()) {
                double distance=Math.hypot(viewer.getX()-center.x,viewer.getZ()-center.z);
                if(distance>90 || !level.hasChunkAt(viewer.blockPosition()))continue;
                Vec3 wind=WindEngine.getWind(viewer.blockPosition(),level);
                double length=Math.max(.001,wind.horizontalDistance());
                // PM wind determines heading; a calm cell still has squall gusts.
                double x=length<.04?Math.cos(storm.tickCount*.08):wind.x/length;
                double z=length<.04?Math.sin(storm.tickCount*.08):wind.z/length;
                for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,viewer.getBoundingBox().inflate(12))) {
                    if(entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator()))continue;
                    if(Math.hypot(entity.getX()-center.x,entity.getZ()-center.z)>90)continue;
                    entity.push(x*.18,.018,z*.18);entity.hurtMarked=true;
                    if(age%4==0 && level.canSeeSky(entity.blockPosition()) && level.random.nextInt(5)==0)
                        entity.hurt(level.damageSources().generic(),1f);
                }
            }
        }
    }
    /** Surface whirlpool: PM foam and authoritative water-only entity movement.
     * It does not claim to be a PMWeather-native fluid renderer. */
    private static final class Whirlpool {
        final Vec3 center;int age;
        Whirlpool(Vec3 center){this.center=center;}
        void tick(ServerLevel level) {
            for(int i=0;i<12;i++) {
                double angle=i*Math.PI/6+age*.22, radius=2.5+(i%3)*1.8;
                double x=center.x+Math.cos(angle)*radius,z=center.z+Math.sin(angle)*radius;
                level.sendParticles(ModParticleTypes.FOAM.get(),x,center.y+.1,z,2,.1,.1,.1,.01);
            }
            if(age%4!=0)return;
            for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                    new AABB(center,center).inflate(7,3,7), e->e.isInWater() && e.isAlive())) {
                if(entity instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()))continue;
                double dx=center.x-entity.getX(),dz=center.z-entity.getZ(),dist=Math.hypot(dx,dz);
                if(dist<.2 || dist>6.5)continue;
                entity.push(dx/dist*.045-dz/dist*.09,-.018,dz/dist*.045+dx/dist*.09);
                entity.hurtMarked=true;
            }
        }
    }
    private static final class HailField {
        final Vec3 center;int age;
        HailField(Vec3 center){this.center=center;}
        void tick(ServerLevel level) {
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL,center.x,center.y+14,center.z,36,13,3,13,.04);
            if(age%4!=0)return;
            for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(16,6,16))) {
                if(entity instanceof ServerPlayer player && player.isCreative())continue;
                if(level.canSeeSky(entity.blockPosition()) && level.random.nextInt(4)==0)
                    entity.hurt(level.damageSources().generic(),1.0f);
            }
        }
    }
    private void seaEffects(ServerLevel level,ServerPlayer player,Vec3 wind,double speed) {
        if(speed<.25)return;
        // Weather-driven ocean crests: floating players, mobs and boats are pushed
        // by the same authoritative PMWeather wind, rather than client-only animation.
        for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,player.getBoundingBox().inflate(24),e->e.isInWater())) {
            double phase=ticks*.055+entity.getX()*.31+entity.getZ()*.19;
            double height=Math.min(.10,speed*.065);
            entity.push(wind.x*.08,Math.cos(phase)*height,wind.z*.08);
            entity.hurtMarked=true;
        }
        for(int i=0;i<24;i++) {
            BlockPos pos=player.blockPosition().offset(level.random.nextInt(40)-20,0,level.random.nextInt(40)-20);
            if(!level.hasChunkAt(pos))continue;
            BlockPos surface=level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR,pos);
            int sea=level.getSeaLevel();
            if(!level.getFluidState(new BlockPos(pos.getX(),sea-1,pos.getZ())).is(FluidTags.WATER))continue;
            double phase=ticks*.05+pos.getX()*.22+pos.getZ()*.17;
            if(Math.sin(phase)>.82)
                level.sendParticles(ModParticleTypes.FOAM.get(),pos.getX()+.5,sea+.1,pos.getZ()+.5,3,1,.2,1,.03);
        }
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
            if(!level.isDay())sky.sprite(level,pos);
        } else {
            // Rare ball lightning moves through the air and lightly shocks entities.
            sky.spawnBall(level,pos.getCenter().add(0,4,0));
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
        if(level.random.nextInt(12)==0)sky.rainbow(level,player.position());
    }
    private enum Kind { DUST_DEVIL, WATERSPOUT, LANDSPOUT, SNOW_WHIRL, FIRE_WHIRL }
    private static final class Whirl {
        Vec3 pos;final Kind kind;final int life;int age;
        Whirl(Vec3 pos,Kind kind,int life){this.pos=pos;this.kind=kind;this.life=life;}
        void tick(ServerLevel level,long clock) {
            Vec3 wind=WindEngine.getWind(BlockPos.containing(pos),level);
            pos=pos.add(wind.x*.5,0,wind.z*.5);
            for(int i=0;i<14;i++) {
                double h=level.random.nextDouble()*16,phase=clock*.25+h*.5+i*2.4;
                double r=.8+h*.1;
                var particle=kind==Kind.WATERSPOUT?EWParticles.WATER.get():kind==Kind.SNOW_WHIRL?EWParticles.SNOW.get():kind==Kind.FIRE_WHIRL?ModParticleTypes.FLAME.get():kind==Kind.DUST_DEVIL?EWParticles.DUST.get():EWParticles.LAND.get();
                level.sendParticles(particle,pos.x+Math.cos(phase)*r,pos.y+h,pos.z+Math.sin(phase)*r,1,0,.1,0,.02);
            }
            if(clock%20==0)for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos,pos.add(0,15,0)).inflate(3))) {
                if(entity instanceof ServerPlayer player && player.isCreative())continue;
                double dx=entity.getX()-pos.x,dz=entity.getZ()-pos.z;
                entity.push(-dz*.035,.12,dx*.035);entity.hurtMarked=true;
                if(kind==Kind.FIRE_WHIRL)entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(),60));
            }
        }
    }
}
