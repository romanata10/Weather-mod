package com.extremeweather;

import com.extremeweather.event.EWEventHandler;
import com.extremeweather.network.EWNetwork;
import com.extremeweather.registry.EWBlockEntities;
import com.extremeweather.registry.EWBlocks;
import com.extremeweather.registry.EWEntities;
import com.extremeweather.registry.EWItems;
import com.extremeweather.registry.EWMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point of the "Extreme Weather: Storms &amp; Disasters" mod.
 *
 * <p>The mod is split in two clearly separated halves:</p>
 * <ul>
 *     <li><b>server</b> - a full weather simulation ({@code com.extremeweather.weather}) that owns
 *     storm cells, vortices, wildfires, floods, tsunamis and all gameplay effects.</li>
 *     <li><b>client</b> - a cinematic renderer ({@code com.extremeweather.client}) that draws
 *     clouds, funnels, rainbows, waves, sprites and haze from the data synced by the server.</li>
 * </ul>
 */
@Mod(ExtremeWeather.MODID)
public class ExtremeWeather {

    public static final String MODID = "extremeweather";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExtremeWeather() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        EWBlocks.BLOCKS.register(modBus);
        EWBlockEntities.BLOCK_ENTITIES.register(modBus);
        EWItems.ITEMS.register(modBus);
        EWItems.CREATIVE_TABS.register(modBus);
        EWEntities.ENTITY_TYPES.register(modBus);
        EWMenus.MENUS.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EWConfig.SPEC, "extremeweather-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EWClientConfig.SPEC, "extremeweather-client.toml");

        MinecraftForge.EVENT_BUS.register(new EWEventHandler());
        MinecraftForge.EVENT_BUS.register(new com.extremeweather.event.EWCommands());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.extremeweather.client.EWClientSetup.init(modBus));

        LOGGER.info("[Extreme Weather] loading weather simulation for Minecraft 1.20.1 / Forge");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(EWNetwork::register);
    }
}
