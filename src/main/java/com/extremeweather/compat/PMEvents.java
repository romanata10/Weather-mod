package com.extremeweather.compat;

import com.extremeweather.fire.WildfireManager;
import net.nullved.pmweatherapi.event.StormEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/** React to PMWeatherAPI events, without generating competing particle funnels or clouds. */
public final class PMEvents {
    @SubscribeEvent public void onNewStorm(StormEvent.New event) {
        if (!event.isClientSide() && event.getStorm().level instanceof ServerLevel level) {
            // Fires spawned by PMWeather's lightning can grow into a wildfire.
            // Weather systems themselves remain under PMWeather's server authority.
            BlockPos center = BlockPos.containing(event.getStorm().position);
            WildfireManager.onStorm(level, center);
        }
    }
}
