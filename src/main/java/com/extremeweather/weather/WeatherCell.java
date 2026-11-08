package com.extremeweather.weather;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import java.util.UUID;

public class WeatherCell {
    public UUID id = UUID.randomUUID();
    public CellType type;
    public double x,z, vx,vz;
    public float intensity, radius;
    public int age, life;
    public WeatherCell(CellType type, double x, double z) {
        this.type=type; this.x=x; this.z=z;
        intensity=type.baseIntensity; radius=32*type.radiusScale;
        life=type.baseLifetime;
    }
    public boolean contains(double px,double pz) { return Math.hypot(px-x,pz-z)<radius; }
    public void tick() { age++; x+=vx; z+=vz; intensity=type.baseIntensity*Mth.clamp(Math.min(age/250f,(life-age)/450f),0f,1f); }
    public boolean expired() { return age>=life; }
    public CompoundTag save() {
        CompoundTag tag=new CompoundTag(); tag.putUUID("Id",id); tag.putString("Type",type.name());
        tag.putDouble("X",x);tag.putDouble("Z",z);tag.putDouble("VX",vx);tag.putDouble("VZ",vz);
        tag.putFloat("Intensity",intensity);tag.putFloat("Radius",radius);tag.putInt("Age",age);tag.putInt("Life",life);return tag;
    }
    public static WeatherCell load(CompoundTag tag) {
        CellType type=CellType.byName(tag.getString("Type"));
        WeatherCell c=new WeatherCell(type==null?CellType.THUNDERSTORM:type,tag.getDouble("X"),tag.getDouble("Z"));
        if(tag.hasUUID("Id")) c.id=tag.getUUID("Id");
        c.vx=tag.getDouble("VX");c.vz=tag.getDouble("VZ");c.intensity=tag.getFloat("Intensity");
        c.radius=tag.getFloat("Radius");c.age=tag.getInt("Age");c.life=tag.getInt("Life");return c;
    }
}
