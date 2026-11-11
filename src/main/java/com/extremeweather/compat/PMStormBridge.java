package com.extremeweather.compat;

import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.Vorticy;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import com.extremeweather.vortex.FunnelShape;
import dev.protomanly.pmweather.weather.storms.StormType;
import dev.protomanly.pmweather.weather.storms.StormTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.nullved.pmweatherapi.storm.NearbyStorms;
import net.nullved.pmweatherapi.storm.StormBuilder;

/**
 * Native PMWeather storms, rendered and synchronized by PMWeather itself.
 * No cloned cloud/tornado textures, separate packets or duplicate storm entities.
 */
public final class PMStormBridge {
    private PMStormBridge() {}

    public static Storm spawn(ServerLevel level, StormType type, Vec3 location, float risk, int stage) {
        if(type==null || !level.hasChunkAt(BlockPos.containing(location)))return null;
        if(NearbyStorms.get(level).stormsNearBlock(BlockPos.containing(location),256).size()>=5)return null;
        // Preserve PMWeather's default wind/geometry behavior. Stage is bounded to
        // the tested range; PMWeather owns subsequent growth and client sync.
        int maxStage=type==StormTypes.SUPERCELL?5:2;
        return new StormBuilder(level,type,location).risk(Math.max(0f,Math.min(1f,risk)))
            .maxStage(maxStage).stage(Math.max(0,Math.min(maxStage,stage))).buildAndSpawn();
    }
    /**
     * Native PMWeather funnel: its actual shader/particles, weather and wind are
     * controlled by PMWeather. Its public tornadoShape is a continuous parameter
     * (initialised by PM to 6..16), NOT an enum of the 23 outline categories.
     */
    public static Storm nativeFunnel(ServerLevel level,Vec3 location,FunnelShape outline,int ef) {
        int rating=Math.max(0,Math.min(5,ef));
        if(!level.hasChunkAt(BlockPos.containing(location)) ||
            NearbyStorms.get(level).stormsNearBlock(BlockPos.containing(location),256).size()>=5)return null;
        float width=(16+rating*10f)*outline.nativeWidthScale();
        int windspeed=65+rating*30;
        // StormBuilder defaults energy AND windspeed/maxWindspeed to zero. A
        // high stage/width alone therefore makes a broad cloud but does not
        // seed an actual tornadic wind field. Initialize PM's native dynamics
        // before syncStormNew instead of trying to add a second addon funnel.
        Storm storm=new StormBuilder(level,StormTypes.SUPERCELL,location)
            .risk(Math.min(1f,.5f+rating*.1f)).stage(Math.max(2,rating)).maxStage(5)
            .energy(500).windspeed(windspeed).maxWindspeed(windspeed)
            .width(width).smoothWidth(width).maxWidth(Math.max(16,Math.round(width)))
            .buildAndSpawn();
        storm.tornadoShape=outline.nativeTaper();
        // Use PMWeather's own satellite-vortex physics and renderer, not a second
        // particle funnel. Constructor parameters: max wind multiplier, relative
        // width, distance from storm axis and life in ticks (verified against JAR).
        // The former 0.68-axis orbit overlapped the main funnel in the joined
        // multi-vortex frame. Give the four requested native satellites room
        // outside the parent's outer wall; no addon funnel is drawn here.
        float orbit=outline==FunnelShape.MULTI_VORTEX?1.12f:.68f;
        float satelliteWidth=outline==FunnelShape.MULTI_VORTEX?.30f:.24f;
        if(storm.vorticies!=null)for(int i=0;i<outline.satellites();i++)
            storm.vorticies.add(new Vorticy(storm,.45f,satelliteWidth,orbit,900+rating*120));
        if(storm.weatherHandler instanceof WeatherHandlerServer handler)handler.syncStormUpdate(storm);
        return storm;
    }
    public static Storm supercell(ServerLevel level,Vec3 location) {
        return spawn(level,StormTypes.SUPERCELL,location,.75f,2);
    }
    public static Storm cyclone(ServerLevel level,Vec3 location) {
        Storm storm=spawn(level,StormTypes.CYCLONE,location,.80f,2);
        if(storm!=null)widenCyclone(storm);
        return storm;
    }
    /** Adopt and widen an existing native cyclone; NEVER spawn a second cloud. */
    public static void widenCyclone(Storm storm) {
        storm.width=Math.max(storm.width,220f);
        storm.smoothWidth=Math.max(storm.smoothWidth,220f);
        storm.maxWidth=Math.max(storm.maxWidth,240);
        if(storm.weatherHandler instanceof WeatherHandlerServer handler)handler.syncStormUpdate(storm);
    }
    public static Storm fireWhirl(ServerLevel level,Vec3 location) {
        return spawn(level,StormTypes.FIRE_WHIRL,location,.65f,1);
    }
}
