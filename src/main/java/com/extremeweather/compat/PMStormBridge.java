package com.extremeweather.compat;

import dev.protomanly.pmweather.weather.Storm;
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
        return new StormBuilder(level,type,location).risk(Math.max(0f,Math.min(1f,risk)))
            .stage(Math.max(0,Math.min(2,stage))).buildAndSpawn();
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
