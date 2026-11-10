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
    private final List<Sprite> sprites = new ArrayList<>();

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

    /** Short-lived red sprite above a PMWeather thundercloud. The photo is a
     * reference for crown-and-tendril geometry, not an asset copied into the mod. */
    public boolean sprite(ServerLevel level, BlockPos origin) {
        long time=level.getDayTime()%24000;
        if(time<13000 || time>23000 || sprites.size()>=3)return false;
        // PMWeather places cloud centers high above the terrain: a spherical
        // range check against the player's Y rejects storms directly overhead.
        var cloud=net.nullved.pmweatherapi.storm.NearbyStorms.get(level)
            .stormsNearBlock(origin,2048).stream()
            .filter(storm->Math.hypot(storm.position.x-origin.getX(),storm.position.z-origin.getZ())<220)
            .min(java.util.Comparator.comparingDouble(storm->Math.hypot(storm.position.x-origin.getX(),storm.position.z-origin.getZ())))
            .orElse(null);
        if(cloud==null)return false;
        Sprite sprite=new Sprite(cloud,Vec3.atCenterOf(origin));
        sprites.add(sprite);
        emitSprite(level,sprite);
        return true;
    }

    /** Re-emit a bounded cloud-anchored crown and crooked tendrils for three
     * seconds: one-shot vanilla dust particles disappear before players can
     * turn toward a sprite. This remains particle geometry, not PM's shader. */
    private void emitSprite(ServerLevel level,Sprite sprite) {
        double x0=sprite.cloud.position.x,z0=sprite.cloud.position.z;
        double crownY=Math.min(level.getMaxBuildHeight()-8,Math.max(170,sprite.cloud.position.y+82));
        var red=new DustParticleOptions(new Vector3f(1f,.07f,.11f),2f);
        var pink=new DustParticleOptions(new Vector3f(1f,.27f,.20f),1.25f);
        for(int strand=0;strand<9;strand++) {
            double offset=(strand-4)*3.0;
            double zoffset=Math.sin(strand*2.7)*5;
            for(int step=0;step<22;step+=2) {
                double t=step/21d;
                double x=x0+offset+Math.sin(t*9+strand)*t*3;
                double y=crownY-t*(36+(strand%4)*4);
                double z=z0+zoffset+Math.cos(t*7+strand)*t*2;
                broadcastSky(level,sprite.focus,step<6?red:pink,x,y,z,step<5?2:1,.8,.5,.8,.01);
                if(step>=6 && step<16 && step%4==2)for(int twig=1;twig<=3;twig++)
                    broadcastSky(level,sprite.focus,pink,x+(twig*1.7)*(strand%2==0?1:-1),y-twig*1.3,z+twig*.8,1,.2,.1,.2,0);
            }
            broadcastSky(level,sprite.focus,red,x0+offset,crownY+3,z0+zoffset,5,2,3,2,.012);
        }
    }
    private static final class Sprite {
        final dev.protomanly.pmweather.weather.Storm cloud;
        final Vec3 focus;
        int age;
        Sprite(dev.protomanly.pmweather.weather.Storm cloud,Vec3 focus){this.cloud=cloud;this.focus=focus;}
    }
    public int activeSprites(){return sprites.size();}

    /** Normal server particle delivery culls after 32 blocks; bows and sprites
     * are 60–110 blocks away from the player, so request long-distance delivery. */
    private static void broadcastSky(ServerLevel level,Vec3 origin,ParticleOptions particle,
                                     double x,double y,double z,int count,double dx,double dy,double dz,double speed) {
        for(ServerPlayer player:level.players())if(player.position().distanceToSqr(origin)<256*256)
            level.sendParticles(player,particle,true,x,y,z,count,dx,dy,dz,speed);
    }

    public void spawnBall(ServerLevel level, Vec3 position) {
        if(balls.size()<8)balls.add(new Ball(position,level.random.nextDouble()*Math.PI*2));
    }
    public void clear(){balls.clear();sprites.clear();}
    public boolean canSpawnBall(){return balls.size()<8;}
    public void tick(ServerLevel level) {
        for(Iterator<Sprite> it=sprites.iterator();it.hasNext();) {
            Sprite sprite=it.next();
            if(++sprite.age>30 || sprite.cloud.dead) {it.remove();continue;}
            if(sprite.age%2==0)emitSprite(level,sprite);
        }
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
