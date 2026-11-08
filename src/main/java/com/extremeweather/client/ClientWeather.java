package com.extremeweather.client;
import com.extremeweather.network.WeatherPacket;
import java.util.List;
public final class ClientWeather {
 public static List<WeatherPacket.Cell> cells=List.of();
 public static List<WeatherPacket.Funnel> funnels=List.of();
 public static long time;
 public static void accept(WeatherPacket packet){cells=List.copyOf(packet.cells);funnels=List.copyOf(packet.funnels);time=packet.time;}
 private ClientWeather(){}
}
