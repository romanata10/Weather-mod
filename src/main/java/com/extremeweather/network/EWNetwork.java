package com.extremeweather.network;
import com.extremeweather.ExtremeWeather;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
public final class EWNetwork {
 private static final String VERSION="1";
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ExtremeWeather.MODID,"main"),()->VERSION,VERSION::equals,VERSION::equals);
 public static void register(){ CHANNEL.registerMessage(0,WeatherPacket.class,WeatherPacket::encode,WeatherPacket::decode,WeatherPacket::handle); }
 private EWNetwork(){}
}
