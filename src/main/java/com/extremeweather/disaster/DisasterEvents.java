package com.extremeweather.disaster;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.Map;
import java.util.WeakHashMap;
public final class DisasterEvents {
 private static final Map<ServerLevel,DisasterEngine> engines=new WeakHashMap<>();
 public static DisasterEngine get(ServerLevel level){return engines.computeIfAbsent(level,k->new DisasterEngine());}
 // PMWeather may rewrite wind and width in its own tick. Apply addon bounds
 // after normal-priority listeners, on the same authoritative server level.
 @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(LevelTickEvent.Post event){
  if(event.getLevel() instanceof ServerLevel level)get(level).tick(level);
 }
}
