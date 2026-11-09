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
        Storm storm=spawn(level,StormTypes.SUPERCELL,location,Math.min(1f,.5f+rating*.1f),Math.max(2,rating));
        if(storm==null)return null;
        float width=16+rating*10f;
        if(outline==FunnelShape.WEDGE || outline==FunnelShape.BOWL_SHAPED)width*=1.7f;
        if(outline==FunnelShape.EL_RENO)width*=2.6f;
        if(outline==FunnelShape.WIRE || outline==FunnelShape.ROPE || outline==FunnelShape.NEEDLE)width*=.48f;
        storm.width=width;
        storm.smoothWidth=width;
        storm.maxWidth=Math.max(16,Math.round(width));
        storm.maxWindspeed=65+rating*30;
        storm.tornadoShape=6f+outline.ordinal()*10f/(FunnelShape.values().length-1);
        // Use PMWeather's own satellite-vortex physics and renderer, not a second
        // particle funnel. Constructor parameters: max wind multiplier, relative
        // width, distance from storm axis and life in ticks (verified against JAR).
        if(storm.vorticies!=null)for(int i=0;i<outline.satellites();i++)
            storm.vorticies.add(new Vorticy(storm,.35f,.24f,.68f,900+rating*120));
        if(storm.weatherHandler instanceof WeatherHandlerServer handler)handler.syncStormUpdate(storm);
        return storm;
    }
    public static Storm supercell(ServerLevel level,Vec3 location) {
        return spawn(level,StormTypes.SUPERCELL,location,.75f,2);
    }
    public static Storm cyclone(ServerLevel level,Vec3 location) {
        return spawn(level,StormTypes.CYCLONE,location,.80f,2);
    }
    public static Storm fireWhirl(ServerLevel level,Vec3 location) {
        return spawn(level,StormTypes.FIRE_WHIRL,location,.65f,1);
    }
}
