package com.extremeweather.compat;

import net.nullved.pmweatherapi.event.StormEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.server.level.ServerLevel;

/** React to PMWeatherAPI events, without generating competing particle funnels or clouds. */
public final class PMEvents {
    @SubscribeEvent public void onNewStorm(StormEvent.New event) {
        if (!event.isClientSide() && event.getStorm().level instanceof ServerLevel level) {
            // Native storm creation is owned by PMWeather. Effects are sampled by
            // DisasterEngine using NearbyStorms after this event, when the storm ID
            // has been assigned and PMWeather has synchronized it to clients.
        }
    }
}
