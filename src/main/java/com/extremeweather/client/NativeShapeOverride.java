package com.extremeweather.client;

import com.extremeweather.ExtremeWeather;
import com.extremeweather.compat.FunnelShapePacket;
import dev.protomanly.pmweather.weather.storms.StormTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.nullved.pmweatherapi.storm.NearbyStorms;
import java.util.HashMap;
import java.util.Map;

/** Reapply the SERVER-selected taper to the SAME PMWeather client storm.
 * PMWeather may change its local tornadoShape between native sync packets. */
@EventBusSubscriber(modid=ExtremeWeather.MODID,value=Dist.CLIENT)
public final class NativeShapeOverride {
    private record Target(float taper,long expires) {}
    private static final Map<Long,Target> targets=new HashMap<>();
    private static ClientLevel world;
    private static boolean narrowLogged,wideLogged;
    private NativeShapeOverride() {}
    public static void receive(FunnelShapePacket packet) {
        Minecraft game=Minecraft.getInstance();
        if(game.level==null || !Float.isFinite(packet.taper()))return;
        if(world!=game.level){targets.clear();world=game.level;narrowLogged=false;wideLogged=false;}
        targets.put(packet.stormId(),new Target(packet.taper(),game.level.getGameTime()+60));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(ClientTickEvent.Post event) {
        Minecraft game=Minecraft.getInstance();
        if(game.level!=world){targets.clear();world=game.level;narrowLogged=false;wideLogged=false;}
        if(world==null || game.player==null || targets.isEmpty() ||
            dev.protomanly.pmweather.event.GameBusClientEvents.getClientWeather()==null)return;
        long now=world.getGameTime();
        targets.entrySet().removeIf(e->e.getValue().expires()<now);
        if(targets.isEmpty())return;
        for(var storm:NearbyStorms.client().stormsNearPlayer(game.player,512)) {
            Target target=targets.get(storm.ID);
            if(target!=null && !storm.dead && storm.is(StormTypes.SUPERCELL)) {
                storm.tornadoShape=target.taper();
                boolean narrow=storm.width>=8 && storm.width<35,wide=storm.width>=75;
                if((narrow && !narrowLogged) || (wide && !wideLogged)) {
                    if(narrow)narrowLogged=true;
                    if(wide)wideLogged=true;
                    com.mojang.logging.LogUtils.getLogger().info(
                        "ExtremeWeather joined client applied authoritative taper: width={} taper={}",
                        storm.width,storm.tornadoShape);
                }
            }
        }
    }
}
