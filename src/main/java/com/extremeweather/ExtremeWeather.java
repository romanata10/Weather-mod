package com.extremeweather;

import com.extremeweather.block.EWBlocks;
import com.extremeweather.fire.FireEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Addon: PMWeather owns the clouds, storms, tornadoes and multiplayer weather sync. */
@Mod(ExtremeWeather.MODID)
public final class ExtremeWeather {
    public static final String MODID = "extremeweather";
    public ExtremeWeather(IEventBus modBus, ModContainer container) {
        EWBlocks.BLOCKS.register(modBus);
        EWBlocks.ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(new FireEvents());
        NeoForge.EVENT_BUS.register(new com.extremeweather.disaster.DisasterEvents());
        NeoForge.EVENT_BUS.register(new com.extremeweather.command.EWCommands());
    }
}
