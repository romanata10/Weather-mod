package com.extremeweather.network;
import com.extremeweather.weather.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;
public class WeatherPacket {
 public record Cell(double x,double z,float intensity,float radius,CellType type) {}
 public record Funnel(double x,double y,double z,float width,float height,float rotation,float ef,VortexType type,VortexShape shape,int subs) {}
 public final List<Cell> cells=new ArrayList<>();public final List<Funnel> funnels=new ArrayList<>();public long time;
 public WeatherPacket(){}
 public WeatherPacket(List<WeatherCell> storms,List<Vortex> vortices,long time){
  this.time=time;for(WeatherCell c:storms)cells.add(new Cell(c.x,c.z,c.intensity,c.radius,c.type));
  for(Vortex v:vortices)funnels.add(new Funnel(v.x,v.y,v.z,v.width,v.height,v.rotation,v.ef,v.type,v.shape,v.subs));
 }
 public static void encode(WeatherPacket p,FriendlyByteBuf b) {
  b.writeLong(p.time);b.writeVarInt(p.cells.size());
  for(Cell c:p.cells){b.writeDouble(c.x);b.writeDouble(c.z);b.writeFloat(c.intensity);b.writeFloat(c.radius);b.writeEnum(c.type);}
  b.writeVarInt(p.funnels.size());
  for(Funnel f:p.funnels){b.writeDouble(f.x);b.writeDouble(f.y);b.writeDouble(f.z);b.writeFloat(f.width);b.writeFloat(f.height);b.writeFloat(f.rotation);b.writeFloat(f.ef);b.writeEnum(f.type);b.writeEnum(f.shape);b.writeVarInt(f.subs);}
 }
 public static WeatherPacket decode(FriendlyByteBuf b){
  WeatherPacket p=new WeatherPacket();p.time=b.readLong();int count=Math.min(b.readVarInt(),128);
  for(int i=0;i<count;i++)p.cells.add(new Cell(b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat(),b.readEnum(CellType.class)));
  count=Math.min(b.readVarInt(),128);
  for(int i=0;i<count;i++)p.funnels.add(new Funnel(b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readEnum(VortexType.class),b.readEnum(VortexShape.class),b.readVarInt()));
  return p;
 }
 public static void handle(WeatherPacket p,Supplier<NetworkEvent.Context> context){
  context.get().enqueueWork(()->com.extremeweather.client.ClientWeather.accept(p));context.get().setPacketHandled(true);
 }
}
