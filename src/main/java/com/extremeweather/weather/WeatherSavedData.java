package com.extremeweather.weather;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class WeatherSavedData extends SavedData {
    public final WeatherEngine engine=new WeatherEngine();
    public static WeatherSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WeatherSavedData::load,WeatherSavedData::new,"extremeweather");
    }
    public static WeatherSavedData load(CompoundTag tag) { WeatherSavedData data=new WeatherSavedData();data.engine.load(tag);return data; }
    @Override public CompoundTag save(CompoundTag tag) {return engine.save(tag);}
}
