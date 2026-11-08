package com.extremeweather.event;
import com.extremeweather.weather.WeatherSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public class EWEventHandler {
 @SubscribeEvent public void onLevelTick(TickEvent.LevelTickEvent event) {
  if(event.phase==TickEvent.Phase.END && event.level instanceof ServerLevel level) WeatherSavedData.get(level).engine.tick(level);
 }
}
