package com.extremeweather.client;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
public final class EWClientSetup {
 public static void init(IEventBus bus){MinecraftForge.EVENT_BUS.register(new WeatherVisuals());}
 private EWClientSetup(){}
}
