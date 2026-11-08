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
    private final Map<BlockPos,Integer> burning = new HashMap<>();
    private int clock;
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
        int budget=48;
        Iterator<Map.Entry<BlockPos,Integer>> iter=burning.entrySet().iterator();
        List<BlockPos> spread=new ArrayList<>();
        while(iter.hasNext() && budget-->0) {
            var entry=iter.next(); BlockPos p=entry.getKey();int age=entry.getValue()+10;
            if (!level.hasChunkAt(p)) {iter.remove();continue;}
            if (level.isRainingAt(p.above()) || level.getFluidState(p).isSource() || age>700) {iter.remove();continue;}
            entry.setValue(age);
            if (age>130 && age%40==0) {
                for(int i=0;i<3;i++) {
                    BlockPos n=p.offset(level.random.nextInt(5)-2,level.random.nextInt(3)-1,level.random.nextInt(5)-2);
                    if(level.hasChunkAt(n) && isFuel(level.getBlockState(n)) && !burning.containsKey(n))spread.add(n);
                }
            }
            if(age>200 && (age%90==0 || age>650)) {
                BlockState old=level.getBlockState(p);
                if(old.is(Blocks.GRASS_BLOCK) || old.is(BlockTags.DIRT) || old.is(Blocks.MOSS_BLOCK))
                    level.setBlock(p, EWBlocks.SCORCHED_EARTH.get().defaultBlockState(),3);
                else if(old.is(BlockTags.LEAVES) || old.is(Blocks.SHORT_GRASS) || old.is(Blocks.TALL_GRASS))
                    level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
                if(age>650) {iter.remove();continue;}
            }
            if(clock%20==0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE,p.getX()+.5,p.getY()+2,p.getZ()+.5,6,1,2,1,.06);
                level.sendParticles(ParticleTypes.FLAME,p.getX()+.5,p.getY()+1,p.getZ()+.5,4,.5,.8,.5,.02);
            }
        }
        for(BlockPos p:spread)ignite(level,p);
        if(clock%40==0) smoke(level);
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
    public static void onStorm(ServerLevel level,BlockPos pos) { /* PMWeather owns storm formation. */ }
}
