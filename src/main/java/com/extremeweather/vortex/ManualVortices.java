package com.extremeweather.vortex;

import dev.protomanly.pmweather.weather.WindEngine;
import com.extremeweather.particle.EWParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Explicitly summoned addon funnels: server physics plus a lightweight shape silhouette. */
public final class ManualVortices {
    private final List<Funnel> funnels=new ArrayList<>();
    public int count(){return funnels.size();}
    public boolean spawn(ServerLevel level,Vec3 pos,FunnelShape shape,int ef) {
        if(funnels.size()>=4 || !level.hasChunkAt(BlockPos.containing(pos)))return false;
        funnels.add(new Funnel(pos,shape,Math.max(0,Math.min(5,ef))));return true;
    }
    public void clear(){funnels.clear();}
    public void tick(ServerLevel level) {
        for(Iterator<Funnel> it=funnels.iterator();it.hasNext();) {
            Funnel f=it.next();if(++f.age>f.life || !level.hasChunkAt(BlockPos.containing(f.pos))){it.remove();continue;}
            if(f.age%8==0)f.render(level);
            if(f.age%10==0)f.wind(level);
        }
    }
    /** Tall PMWeather-textured silhouettes must not disappear at the normal
     * 32-block server particle culling radius. Only nearby viewers receive them. */
    private static void sendVisible(ServerLevel level,Vec3 origin,ParticleOptions particle,
                                    double x,double y,double z,int count,double dx,double dy,double dz,double speed) {
        for(ServerPlayer viewer:level.players())if(viewer.position().distanceToSqr(origin)<256*256)
            level.sendParticles(viewer,true,particle,x,y,z,count,dx,dy,dz,speed);
    }
    private static final class Funnel {
        Vec3 pos;final FunnelShape shape;final int ef;int age;final int life;
        Funnel(Vec3 pos,FunnelShape shape,int ef) {this.pos=pos;this.shape=shape;this.ef=ef;this.life=1200+ef*300;}
        double width(){return (3+ef*2.8)*(shape==FunnelShape.EL_RENO?2.3:1);}
        void render(ServerLevel level) {
            double height=35+ef*13;
            // Deliberately bounded packet count: one dust/cloud batch per ring.
            for(int ring=0;ring<12;ring++) {
                double h=ring/11d,r=shape.radius(h,width(),age);
                double lean=shape==FunnelShape.DEAD_MAN_WALKING?Math.sin(age*.025+h*7)*8*h:shape==FunnelShape.LOOPING?Math.sin(h*8+age*.03)*3:0;
                double phase=age*.09+ring*.35;
                double x=pos.x+lean+Math.cos(phase)*r,z=pos.z+Math.sin(phase)*r;
                sendVisible(level,pos,EWParticles.LAND.get(),x,pos.y+h*height,z,4,Math.min(3,r*.15),1.8,Math.min(3,r*.15),.025);
                if(ring<4)sendVisible(level,pos,EWParticles.DUST.get(),x,pos.y+h*height,z,3,Math.min(3,r*.12),1,Math.min(3,r*.12),.02);
                for(int satellite=0;satellite<shape.satellites();satellite++) {
                    double angle=age*.11+satellite*Math.PI*2/shape.satellites()+h*4;
                    double orbit=r*.68;
                    sendVisible(level,pos,EWParticles.LAND.get(),pos.x+Math.cos(angle)*orbit,pos.y+h*height,pos.z+Math.sin(angle)*orbit,2,.5,.5,.5,.02);
                }
            }
        }
        void wind(ServerLevel level) {
            Vec3 wind=WindEngine.getWind(BlockPos.containing(pos),level);
            pos=pos.add(wind.x*.35,0,wind.z*.35);
            double reach=Math.min(55,width()*2.5+10);
            for(Entity e:level.getEntitiesOfClass(Entity.class,new AABB(pos,pos.add(0,35+ef*13,0)).inflate(reach),Entity::isAlive)) {
                if(e instanceof Player p && (p.isCreative() || p.isSpectator()))continue;
                double dx=e.getX()-pos.x,dz=e.getZ()-pos.z,d=Math.hypot(dx,dz)+.01;
                if(d>reach)continue;
                double force=(1-d/reach)*(.09+ef*.045);
                e.push(-dz/d*force-dx/d*force*.3,.04+force*.55,dx/d*force-dz/d*force*.3);
                e.hurtMarked=true;e.fallDistance=0;
            }
        }
    }
}
