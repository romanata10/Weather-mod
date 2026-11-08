package com.extremeweather.weather;

import com.extremeweather.EWConfig;
import com.extremeweather.network.EWNetwork;
import com.extremeweather.network.WeatherPacket;
import com.extremeweather.util.EWDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** Persistent, bounded, per-dimension server weather simulation. */
public class WeatherEngine {
    private final List<WeatherCell> cells=new ArrayList<>();
    private final List<Vortex> vortices=new ArrayList<>();
    private final Set<BlockPos> floodWater=new HashSet<>();
    private int tick;
    public List<WeatherCell> cells() {return cells;}
    public List<Vortex> vortices() {return vortices;}
    public Vec3 windAt(double x,double z) {
        double wx=0.07,wz=0.03;
        for(WeatherCell c:cells) {
            double dx=x-c.x,dz=z-c.z,d=Math.hypot(dx,dz)+1;
            if(d>c.radius*2)continue;
            double strength=c.intensity*(1-d/(c.radius*2))*0.22;
            wx+=(-dz/d+dx/d*.25)*strength;
            wz+=(dx/d+dz/d*.25)*strength;
        }
        return new Vec3(wx,0,wz);
    }
    public WeatherCell nearest(double x,double z) {
        WeatherCell best=null;double distance=Double.MAX_VALUE;
        for(WeatherCell c:cells) {double d=Math.hypot(x-c.x,z-c.z);if(d<distance){distance=d;best=c;}}
        return best;
    }
    public WeatherCell spawnCell(ServerLevel level,CellType type, BlockPos pos) {
        WeatherCell cell=new WeatherCell(type,pos.getX(),pos.getZ());
        Vec3 wind=windAt(cell.x,cell.z);cell.vx=wind.x;cell.vz=wind.z;
        cells.add(cell);dirty(level);return cell;
    }
    public Vortex spawnVortex(ServerLevel level,VortexType type,BlockPos pos,float ef) {
        Vortex vortex=new Vortex(type,pos.getX()+.5,pos.getY(),pos.getZ()+.5,Mth.clamp(ef,0,5));
        vortices.add(vortex);dirty(level);return vortex;
    }
    public void clear(ServerLevel level) {cells.clear();vortices.clear();cleanupFlood(level);dirty(level);}
    private void dirty(ServerLevel level){WeatherSavedData.get(level).setDirty();}
    public void tick(ServerLevel level) {
        tick++;
        if(tick%2400==0) attemptNaturalSpawn(level);
        if(tick%100==0) attemptSmallVortex(level);
        for(WeatherCell cell:cells) {
            cell.tick();
            if(cell.age%100==0) cellEffects(level,cell);
        }
        cells.removeIf(WeatherCell::expired);
        for(Vortex v:vortices)v.tick(level,this);
        vortices.removeIf(v->!v.isAlive());
        if(tick%400==0 && cells.isEmpty())cleanupFlood(level);
        if(EWConfig.WAVES_AFFECT_ENTITIES.get() && tick%5==0)waveEffects(level);
        if(tick%EWConfig.SYNC_INTERVAL.get()==0)
            EWNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new WeatherPacket(cells,vortices,level.getGameTime()));
        if(tick%200==0)dirty(level);
    }
    private void attemptNaturalSpawn(ServerLevel level) {
        if(!EWConfig.ENABLE_NATURAL_CELLS.get() || cells.size()>=EWConfig.MAX_CELLS_PER_LEVEL.get() || level.players().isEmpty())return;
        if(level.random.nextDouble()>EWConfig.NATURAL_STORM_CHANCE.get())return;
        var player=level.players().get(level.random.nextInt(level.players().size()));
        int x=player.getBlockX()+level.random.nextInt(128)-64,z=player.getBlockZ()+level.random.nextInt(128)-64;
        BlockPos pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
        if(!level.hasChunkAt(pos))return;
        Holder<Biome> biome=level.getBiome(pos);boolean ocean=biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN);
        float temp=biome.value().getBaseTemperature();
        CellType type=ocean?CellType.pickOcean(level.random,temp>0.7f?1.0f:0.4f,EWConfig.ENABLE_TROPICAL_CYCLONES.get()):CellType.pickLand(level.random,temp<0.2f);
        spawnCell(level,type,pos);
    }
    private void attemptSmallVortex(ServerLevel level) {
        if(level.players().isEmpty() || vortices.size()>12)return;
        var player=level.players().get(level.random.nextInt(level.players().size()));
        int x=player.getBlockX()+level.random.nextInt(192)-96,z=player.getBlockZ()+level.random.nextInt(192)-96;
        BlockPos pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
        if(!level.hasChunkAt(pos))return;
        Holder<Biome> biome=level.getBiome(pos);float temp=biome.value().getBaseTemperature();
        WeatherCell cell=nearest(x,z);boolean storm=cell!=null&&cell.contains(x,z);
        if(storm && level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER) && EWConfig.ENABLE_WATERSPOUTS.get() && level.random.nextDouble()<EWConfig.WATERSPOUT_CHANCE.get())
            spawnVortex(level,VortexType.WATERSPOUT,pos,.5f);
        else if(storm && EWConfig.ENABLE_LANDSPOUTS.get() && level.random.nextDouble()<EWConfig.LANDSPOUT_CHANCE.get())
            spawnVortex(level,VortexType.LANDSPOUT,pos,.6f);
        else if(!storm && !level.isRaining() && temp>1.0f && EWConfig.ENABLE_DUST_DEVILS.get() && level.random.nextDouble()<EWConfig.DUST_DEVIL_CHANCE.get())
            spawnVortex(level,VortexType.DUST_DEVIL,pos,.35f);
        else if(!storm && temp<.15f && EWConfig.ENABLE_SNOW_WHIRLS.get() && level.random.nextDouble()<EWConfig.SNOW_WHIRL_CHANCE.get())
            spawnVortex(level,VortexType.SNOW_WHIRL,pos,.4f);
    }
    private void cellEffects(ServerLevel level,WeatherCell c) {
        BlockPos pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(Mth.floor(c.x),0,Mth.floor(c.z)));
        if(!level.hasChunkAt(pos))return;
        if(c.age%600==0 && EWConfig.ENABLE_TORNADOES.get() && c.type.rotating && level.random.nextDouble()<EWConfig.TORNADO_CHANCE.get()*c.type.tornadoRisk) {
            float ef=Mth.clamp((float)(level.random.nextDouble()*4.0+level.random.nextDouble()*1.2),EWConfig.MIN_EF.get().floatValue(),EWConfig.MAX_EF.get().floatValue());
            Vortex v=spawnVortex(level,ef>4.7f?VortexType.HYPERVORTEX:VortexType.TORNADO,pos,ef);v.parent=c.id;
        }
        if(EWConfig.ENABLE_LIGHTNING.get() && level.random.nextInt(4)==0) {
            var lightning=net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if(lightning!=null) {
                BlockPos strike=pos.offset(level.random.nextInt(80)-40,0,level.random.nextInt(80)-40);
                strike=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,strike);
                lightning.moveTo(strike.getX(),strike.getY(),strike.getZ());level.addFreshEntity(lightning);
                if(c.type==CellType.SUPERCELL && EWConfig.ENABLE_HYPER_LIGHTNING.get() && level.random.nextDouble()<EWConfig.HYPER_LIGHTNING_CHANCE.get()) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,strike.getX(),strike.getY()+10,strike.getZ(),100,12,20,12,.4);
                    for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(strike).inflate(8)))
                        entity.hurt(EWDamageTypes.of(level,EWDamageTypes.HYPER_LIGHTNING),12);
                }
                if(EWConfig.ENABLE_WILDFIRES.get() && level.random.nextDouble()<EWConfig.WILDFIRE_CHANCE.get()) {
                    BlockPos fire=strike.above(); if(level.getBlockState(fire).isAir() && level.getBlockState(fire.below()).isFlammable(level,fire.below(),net.minecraft.core.Direction.UP))level.setBlock(fire,Blocks.FIRE.defaultBlockState(),3);
                }
            }
        }
        if(c.type.hurricaneForce && EWConfig.ENABLE_FLOODING.get() && c.age%400==0) flood(level,pos,c);
        if(c.type.hurricaneForce && EWConfig.ENABLE_TSUNAMIS.get() && c.age%1200==0 && level.random.nextDouble()<EWConfig.TSUNAMI_CHANCE.get()) flood(level,pos,c);
        if(EWConfig.ENABLE_HAIL.get() && c.type==CellType.SUPERCELL) {
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL,c.x,pos.getY()+25,c.z,80,30,8,30,.8);
            for(var player:level.players()) {
                if(c.contains(player.getX(),player.getZ()) && level.canSeeSky(player.blockPosition()) && level.random.nextInt(8)==0)
                    player.hurt(EWDamageTypes.of(level,EWDamageTypes.HAIL),EWConfig.HAIL_DAMAGE.get().floatValue());
            }
        }
        if(EWConfig.ENABLE_FIRE_WHIRLS.get() && c.age%1200==0 && level.random.nextDouble()<EWConfig.FIRE_WHIRL_CHANCE.get()) {
            for(int i=0;i<16;i++) {
                BlockPos fire=pos.offset(level.random.nextInt(48)-24,level.random.nextInt(8)-4,level.random.nextInt(48)-24);
                if(level.hasChunkAt(fire) && level.getBlockState(fire).is(Blocks.FIRE)) {
                    spawnVortex(level,VortexType.FIRE_WHIRL,fire,1);break;
                }
            }
        }
        if(EWConfig.ENABLE_BALL_LIGHTNING.get() && level.random.nextDouble()<EWConfig.BALL_LIGHTNING_CHANCE.get())
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,c.x,pos.getY()+3,c.z,80,2,2,2,.08);
        if(EWConfig.ENABLE_SPRITES.get() && c.type==CellType.SUPERCELL && level.random.nextDouble()<EWConfig.SPRITE_CHANCE.get())
            level.sendParticles(ParticleTypes.END_ROD,c.x,pos.getY()+100,c.z,60,15,25,15,.1);
    }
    private void waveEffects(ServerLevel level) {
        for(var player:level.players()) {
            var center=player.blockPosition();
            if(!level.getFluidState(center).is(net.minecraft.tags.FluidTags.WATER))continue;
            WeatherCell c=nearest(center.getX(),center.getZ());
            if(c==null || !c.type.tropical || !c.contains(center.getX(),center.getZ()))continue;
            double height=Math.min(EWConfig.MAX_WAVE_AMPLITUDE.get(),c.intensity*2.5);
            if(!player.isCreative()) {player.push(windAt(center.getX(),center.getZ()).x*.2,Math.sin(tick*.08+center.getX()*.12)*height*.015,windAt(center.getX(),center.getZ()).z*.2);player.hurtMarked=true;}
        }
    }
    private void flood(ServerLevel level,BlockPos center,WeatherCell c) {
        if(floodWater.size()>=EWConfig.FLOOD_BLOCK_LIMIT.get())return;
        for(int i=0;i<48;i++) {
            BlockPos p=center.offset(level.random.nextInt(60)-30,0,level.random.nextInt(60)-30);
            if(!level.hasChunkAt(p))continue;
            p=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,p);
            if(p.getY()>level.getSeaLevel()+2 || !level.getBlockState(p).isAir())continue;
            if(level.getBlockState(p.below()).isSolidRender(level,p.below())) {
                level.setBlock(p,Blocks.WATER.defaultBlockState(),3);floodWater.add(p);
            }
        }
    }
    private void cleanupFlood(ServerLevel level) {
        for(BlockPos p:floodWater)if(level.hasChunkAt(p)&&level.getBlockState(p).is(Blocks.WATER))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        floodWater.clear();
    }
    public CompoundTag save(CompoundTag tag) {
        ListTag list=new ListTag();for(WeatherCell c:cells)list.add(c.save());tag.put("Cells",list);
        list=new ListTag();for(Vortex v:vortices){CompoundTag t=new CompoundTag();v.save(t);list.add(t);}tag.put("Vortices",list);
        list=new ListTag();for(BlockPos p:floodWater){CompoundTag t=new CompoundTag();t.putLong("Pos",p.asLong());list.add(t);}tag.put("Flood",list);return tag;
    }
    public void load(CompoundTag tag) {
        for(var t:tag.getList("Cells",10))cells.add(WeatherCell.load((CompoundTag)t));
        for(var t:tag.getList("Vortices",10))vortices.add(Vortex.load((CompoundTag)t));
        for(var t:tag.getList("Flood",10))floodWater.add(BlockPos.of(((CompoundTag)t).getLong("Pos")));
    }
}
