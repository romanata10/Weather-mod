package com.extremeweather.client;
import com.extremeweather.EWClientConfig;
import com.extremeweather.EWConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Client-side cloud formation and funnel visualisation through budgeted vanilla particles. */
public class WeatherVisuals {
 private net.minecraft.client.multiplayer.ClientLevel lastLevel;
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;
  Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;
  var level=mc.level;if(lastLevel!=level){ClientWeather.cells=java.util.List.of();ClientWeather.funnels=java.util.List.of();lastLevel=level;}var random=level.random;double px=mc.player.getX(),pz=mc.player.getZ();
  if(EWClientConfig.RENDER_CLOUDS.get())for(var c:ClientWeather.cells){
   if(Math.hypot(px-c.x(),pz-c.z())>240)continue;
   for(int i=0;i<EWClientConfig.CLOUD_DETAIL.get();i++){
    double angle=random.nextDouble()*Math.PI*2,r=random.nextDouble()*c.radius();
    level.addParticle(ParticleTypes.CLOUD,c.x()+Math.cos(angle)*r,Math.min(220,level.getMaxBuildHeight()-30)+random.nextDouble()*20,c.z()+Math.sin(angle)*r,0,0,0);
   }
   if(EWClientConfig.RENDER_PRECIPITATION_STREAKS.get() && Math.hypot(px-c.x(),pz-c.z())<c.radius())
    for(int i=0;i<3;i++)level.addParticle(ParticleTypes.RAIN,px+random.nextInt(32)-16,mc.player.getY()+12,pz+random.nextInt(32)-16,0,-1,0);
  }
  // A rainbow fades into view after a nearby storm as the sun comes out.
  if(EWClientConfig.RENDER_RAINBOW.get() && EWConfig.ENABLE_RAINBOWS.get() && !level.isRaining() && level.isDay() && !ClientWeather.cells.isEmpty() && random.nextInt(3)==0){
   var c=ClientWeather.cells.get(0);double d=Math.hypot(px-c.x(),pz-c.z());
   if(d<180 && d>c.radius()*.7){
    double t=random.nextDouble()*Math.PI, arcX=px+Math.cos(t)*30,arcY=mc.player.getY()+Math.sin(t)*22+8;
    for(int i=0;i<4;i++)level.addParticle(ParticleTypes.END_ROD,arcX,arcY-i*.6,pz+45,0,0,0);
   }
  }
  if(EWClientConfig.RENDER_FUNNELS.get())for(var f:ClientWeather.funnels){
   if(Math.hypot(px-f.x(),pz-f.z())>180)continue;
   for(int i=0;i<8;i++){
    float t=random.nextFloat();double angle=random.nextDouble()*Math.PI*2+f.rotation();
    double radius=f.shape().radiusAt(t,f.width(),f.ef());
    var particle=f.type().fire?ParticleTypes.FLAME:f.type()==com.extremeweather.weather.VortexType.SNOW_WHIRL?ParticleTypes.SNOWFLAKE:f.type().water?ParticleTypes.SPLASH:ParticleTypes.CLOUD;
    level.addParticle(particle,f.x()+Math.cos(angle)*radius,f.y()+t*f.height(),f.z()+Math.sin(angle)*radius,Math.cos(angle+.9)*.1,.08,Math.sin(angle+.9)*.1);
   }
  }
 }
}
