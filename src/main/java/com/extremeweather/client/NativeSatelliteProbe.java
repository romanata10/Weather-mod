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
    private static boolean narrowLogged,wideLogged;
    private NativeSatelliteProbe() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(++ticks%20!=0)return;
        Minecraft game=Minecraft.getInstance();
        if(game.level==null || game.player==null || game.screen!=null)return;
        if(GameBusClientEvents.getClientWeather()==null)return;
        var weather=NearbyStorms.client();
        var storms=weather.stormsNearPlayer(game.player,512);
        for(var storm:storms) {
            if(storm.dead || !storm.is(dev.protomanly.pmweather.weather.storms.StormTypes.SUPERCELL)
                || storm.windspeed<1)continue;
            boolean narrow=storm.width>=8 && storm.width<35;
            boolean wide=storm.width>=75;
            if((narrow && !narrowLogged) || (wide && !wideLogged)) {
                if(narrow)narrowLogged=true;
                if(wide)wideLogged=true;
                com.mojang.logging.LogUtils.getLogger().info(
                    "ExtremeWeather client native funnel geometry: width={} taper={} wind={}",
                    storm.width,storm.tornadoShape,storm.windspeed);
            }
        }
        int most=storms.stream().mapToInt(storm->storm.vorticies==null?0:storm.vorticies.size()).max().orElse(0);
        if(!logged && most>=4) {
            logged=true;
            com.mojang.logging.LogUtils.getLogger().info("ExtremeWeather client native sub-vortices synchronized: {}",most);
        }
    }
}
