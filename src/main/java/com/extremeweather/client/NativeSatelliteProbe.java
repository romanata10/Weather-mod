package com.extremeweather.client;

import com.extremeweather.ExtremeWeather;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.nullved.pmweatherapi.storm.NearbyStorms;

/** Diagnostic only: distinguish server-side sub-vortex bookkeeping from
 * actual PMWeather storm state received by the joined Minecraft client. */
@EventBusSubscriber(modid=ExtremeWeather.MODID,value=Dist.CLIENT)
public final class NativeSatelliteProbe {
    private static int ticks;
    private static boolean logged;
    private NativeSatelliteProbe() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(logged || ++ticks%20!=0)return;
        Minecraft game=Minecraft.getInstance();
        if(game.level==null || game.player==null || game.screen!=null)return;
        if(GameBusClientEvents.getClientWeather()==null)return;
        var weather=NearbyStorms.client();
        int most=weather.stormsNearPlayer(game.player,512).stream()
            .mapToInt(storm->storm.vorticies==null?0:storm.vorticies.size()).max().orElse(0);
        if(most>=4) {
            logged=true;
            com.mojang.logging.LogUtils.getLogger().info("ExtremeWeather client native sub-vortices synchronized: {}",most);
        }
    }
}
