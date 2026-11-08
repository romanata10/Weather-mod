package com.extremeweather.disaster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Rare sky optics. Geometry is broadcast from the server so nearby players see one event. */
public final class SkyPhenomena {
    private static final Vector3f[] SPECTRUM = {
        new Vector3f(1f,.12f,.1f), new Vector3f(1f,.43f,.08f),
        new Vector3f(1f,.92f,.16f), new Vector3f(.18f,.85f,.22f),
        new Vector3f(.16f,.47f,1f), new Vector3f(.58f,.24f,.9f)
    };
    private final List<Ball> balls = new ArrayList<>();

    /** Draw a double bow with colored dust rather than uncolored end-rod sparks. */
    public void rainbow(ServerLevel level, Vec3 origin) {
        for(int bow=0;bow<2;bow++) for(int color=0;color<SPECTRUM.length;color++) {
            double radius=(bow==0?42:52)-color*.65;
            var dust=new DustParticleOptions(SPECTRUM[color],1.1f);
            for(int degree=8;degree<=172;degree+=6) {
                double angle=Math.toRadians(degree);
                double x=origin.x+Math.cos(angle)*radius;
                double y=origin.y+5+Math.sin(angle)*radius;
                broadcastSky(level,origin,dust,x,y,origin.z+60,1,0,0,0,0);
            }
        }
    }

    /** A branching red sprite originating above a large thundercloud, visible at night. */
    public void sprite(ServerLevel level, BlockPos origin) {
        double baseY=Math.min(level.getMaxBuildHeight()-65,Math.max(origin.getY()+80,155));
        for(int branch=0;branch<11;branch++) {
            double x=origin.getX()+(branch-5)*2.5;
            double z=origin.getZ()+((branch*17)%9)-4;
            var glow=new DustParticleOptions(branch%3==0?new Vector3f(1f,.32f,.23f):new Vector3f(.75f,.08f,.24f),1.5f);
            for(int i=0;i<11;i++) {
                double y=baseY+25-i*3;
                broadcastSky(level,Vec3.atCenterOf(origin),glow,x+Math.sin(i*.8+branch)*i*.2,y,z,1,.1,.2,.1,0);
            }
        }
        broadcastSky(level,Vec3.atCenterOf(origin),ParticleTypes.END_ROD,origin.getX(),baseY+25,origin.getZ(),20,8,3,8,.01);
    }

    /** Normal server particle delivery culls after 32 blocks; bows and sprites
     * are 60–110 blocks away from the player, so request long-distance delivery. */
    private static void broadcastSky(ServerLevel level,Vec3 origin,ParticleOptions particle,
                                     double x,double y,double z,int count,double dx,double dy,double dz,double speed) {
        for(ServerPlayer player:level.players())if(player.position().distanceToSqr(origin)<256*256)
            level.sendParticles(player,true,particle,x,y,z,count,dx,dy,dz,speed);
    }

    public void spawnBall(ServerLevel level, Vec3 position) {
        if(balls.size()<8)balls.add(new Ball(position,level.random.nextDouble()*Math.PI*2));
    }
    public void clear(){balls.clear();}
    public boolean canSpawnBall(){return balls.size()<8;}
    public void tick(ServerLevel level) {
        Iterator<Ball> it=balls.iterator();
        while(it.hasNext()) {
            Ball ball=it.next();
            if(++ball.age>180 || !level.hasChunkAt(BlockPos.containing(ball.position))) {it.remove();continue;}
            ball.position=ball.position.add(Math.cos(ball.heading)*.12,Math.sin(ball.age*.12)*.045,Math.sin(ball.heading)*.12);
            var bright=new DustParticleOptions(new Vector3f(.42f,.80f,1f),2f);
            level.sendParticles(bright,ball.position.x,ball.position.y,ball.position.z,4,.3,.3,.3,.01);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,ball.position.x,ball.position.y,ball.position.z,3,.45,.45,.45,.01);
            if(ball.age%20==0)for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(ball.position,ball.position).inflate(2))) {
                entity.hurt(level.damageSources().lightningBolt(),2f);
            }
        }
    }
    private static final class Ball {
        Vec3 position;final double heading;int age;
        Ball(Vec3 position,double heading){this.position=position;this.heading=heading;}
    }
}
