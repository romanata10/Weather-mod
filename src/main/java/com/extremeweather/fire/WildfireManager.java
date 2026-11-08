package com.extremeweather.fire;

import com.extremeweather.block.EWBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;
import java.util.*;

/** Budgeted wildfire spread, scorched soil and smoke. All game effects run on the server. */
public final class WildfireManager {
    private final Map<BlockPos,Integer> burning = new LinkedHashMap<>();
    private int clock;
    /** Adopt fires started by vanilla, PMWeather or another mod, not just lightning. */
    public void seedNearby(ServerLevel level, BlockPos center) {
        if (burning.size() >= 384) return;
        // Bounded deterministic search: the old 24 random probes over a 40x12x40
        // volume had almost no chance to find even a visible fire.
        BlockPos.MutableBlockPos probe=new BlockPos.MutableBlockPos();
        for(int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) for(int y=-3;y<=3;y++) {
            probe.set(center.getX()+x,center.getY()+y,center.getZ()+z);
            if(!level.hasChunkAt(probe) || !level.getBlockState(probe).is(Blocks.FIRE))continue;
            BlockPos p=probe.immutable();
            if(isFuel(level.getBlockState(p.below())))ignite(level,p.below());
            for(Direction dir:Direction.Plane.HORIZONTAL) {
                BlockPos n=p.relative(dir);
                if(isFuel(level.getBlockState(n)))ignite(level,n);
            }
            if(burning.size()>=384)return;
        }
    }
    public int count(){return burning.size();}
    public void stop(ServerLevel level) {
        for(BlockPos pos:burning.keySet()) {
            if(level.hasChunkAt(pos) && level.getBlockState(pos.above()).is(Blocks.FIRE))
                level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
        }
        burning.clear();
    }
    public String describe(ServerLevel level,BlockPos pos) {
        return "tracked="+burning.size()+", age="+burning.get(pos)+", tick="+clock+
            ", block="+level.getBlockState(pos)+", fireAbove="+level.getBlockState(pos.above());
    }
    public void ignite(ServerLevel level, BlockPos position) {
        if (!level.hasChunkAt(position) || burning.size() >= 384) return;
        BlockPos ground = position;
        if (level.getBlockState(ground).isAir()) ground=ground.below();
        BlockState state=level.getBlockState(ground);
        if (!isFuel(state)) return;
        if (burning.putIfAbsent(ground,0)==null && level.getBlockState(ground.above()).isAir())
            level.setBlock(ground.above(),Blocks.FIRE.defaultBlockState(),3);
    }
    private static boolean isFuel(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.DIRT) ||
            state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.SHORT_GRASS) ||
            state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.MOSS_BLOCK);
    }
    public void tick(ServerLevel level) {
        if (++clock % 10 != 0 || burning.isEmpty()) return;
        List<BlockPos> positions=new ArrayList<>(burning.keySet());
        List<BlockPos> spread=new ArrayList<>();
        // Rotate through the tracked cells: a busy fire no longer permanently starves
        // entries beyond the first 48 in the iteration order.
        int size=positions.size(), start=(int)(((long)clock/10*48)%size);
        for(int i=0;i<Math.min(48,size);i++) {
            BlockPos p=positions.get((start+i)%size);
            Integer previous=burning.get(p);
            if(previous==null)continue;
            int age=previous+10;
            if(!level.hasChunkAt(p) || level.isRainingAt(p.above()) || level.getFluidState(p).isSource() || age>700) {
                burning.remove(p);continue;
            }
            burning.put(p,age);
            if (age>130 && age%40==0) {
                for(int sample=0;sample<3;sample++) {
                    BlockPos n=p.offset(level.random.nextInt(5)-2,level.random.nextInt(3)-1,level.random.nextInt(5)-2);
                    if(level.hasChunkAt(n) && isFuel(level.getBlockState(n)) && !burning.containsKey(n))spread.add(n);
                }
            }
            if(age>200 && (age%90==0 || age>650)) {
                BlockState old=level.getBlockState(p);
                if(old.is(Blocks.GRASS_BLOCK) || old.is(BlockTags.DIRT) || old.is(Blocks.MOSS_BLOCK))
                    level.setBlock(p,(old.is(Blocks.GRASS_BLOCK)?EWBlocks.CHARRED_GRASS:EWBlocks.SCORCHED_EARTH).get().defaultBlockState(),3);
                else if(old.is(BlockTags.LEAVES) || old.is(Blocks.SHORT_GRASS) || old.is(Blocks.TALL_GRASS))
                    level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
                if(age>650) {burning.remove(p);continue;}
            }
            if(clock%20==0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE,p.getX()+.5,p.getY()+2,p.getZ()+.5,6,1,2,1,.06);
                level.sendParticles(ParticleTypes.FLAME,p.getX()+.5,p.getY()+1,p.getZ()+.5,4,.5,.8,.5,.02);
            }
        }
        for(BlockPos p:spread)ignite(level,p);
        if(clock%40==0) smoke(level);
        if(clock%10==0) fireWhirls(level);
    }
    private void fireWhirls(ServerLevel level) {
        if (burning.size()<18 || level.random.nextInt(14)!=0) return;
        BlockPos center=burning.keySet().iterator().next();
        long nearby=burning.keySet().stream().filter(p->p.distSqr(center)<256).count();
        if(nearby<12)return;
        // PMWeather's own FIRE_WHIRL handles the actual funnel/cloud styling,
        // lifetime, damage and multiplayer synchronization.
        if(clock%400==0)com.extremeweather.compat.PMStormBridge.fireWhirl(level,center.getCenter());
        for(int height=0;height<18;height++) {
            double angle=clock*.11+height*.55;
            double radius=1+height*.12;
            level.sendParticles(ParticleTypes.FLAME,center.getX()+Math.cos(angle)*radius,center.getY()+height,center.getZ()+Math.sin(angle)*radius,3,.35,.25,.35,.04);
            if(height%3==0)level.sendParticles(ParticleTypes.LARGE_SMOKE,center.getX(),center.getY()+height,center.getZ(),3,radius,.4,radius,.04);
        }
        for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AABB(center).inflate(4,9,4)))e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(),60));
    }
    private void smoke(ServerLevel level) {
        if(burning.size()<8)return;
        // Cluster sources into small groups; effect stays local to loaded wildfire chunks.
        Set<Long> visited=new HashSet<>();
        for(BlockPos p:burning.keySet()) {
            long chunk=net.minecraft.world.level.ChunkPos.asLong(p.getX()>>4,p.getZ()>>4);
            if(!visited.add(chunk))continue;
            long count=burning.keySet().stream().filter(q->Math.abs(q.getX()-p.getX())<28 && Math.abs(q.getZ()-p.getZ())<28).count();
            if(count<8)continue;
            double x=p.getX(),y=p.getY()+Math.min(42,12+count),z=p.getZ();
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,x,y,z,(int)Math.min(65,count*2),14,5,14,.02);
            level.sendParticles(ParticleTypes.ASH,x,y-5,z,25,17,8,17,.03);
            for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AABB(x-22,p.getY()-4,z-22,x+22,y,z+22))) {
                if(e.isInWaterRainOrBubble() || e.hasEffect(net.minecraft.world.effect.MobEffects.POISON))continue;
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100,0));
            }
        }
    }
}
