package com.extremeweather.client;

import com.extremeweather.ExtremeWeather;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One diagnostic marker after a joined client has actually reached gameplay.
 * It never changes weather or draws anything. */
@EventBusSubscriber(modid=ExtremeWeather.MODID,value=Dist.CLIENT)
public final class WorldReadyProbe {
    private static boolean logged;
    private static int gameplayTicks;
    private WorldReadyProbe() {}
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft game=Minecraft.getInstance();
        if(game.level==null || game.player==null || game.screen!=null) {
            gameplayTicks=0;
            return;
        }
        if(!logged && ++gameplayTicks>=20) {
            logged=true;
            com.mojang.logging.LogUtils.getLogger().info("ExtremeWeather client gameplay ready for baseline capture");
        }
    }
}
