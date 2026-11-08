package com.extremeweather.registry;
import com.extremeweather.ExtremeWeather;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;
public final class EWItems {
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ExtremeWeather.MODID);
 public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ExtremeWeather.MODID);
 public static final RegistryObject<Item> STORM_ANCHOR=block("storm_anchor",EWBlocks.STORM_ANCHOR);
 public static final RegistryObject<Item> WEATHER_RADAR=block("weather_radar",EWBlocks.WEATHER_RADAR);
 public static final RegistryObject<Item> ANEMOMETER=block("anemometer",EWBlocks.ANEMOMETER);
 public static final RegistryObject<Item> BAROMETER=block("barometer",EWBlocks.BAROMETER);
 public static final RegistryObject<Item> STORM_SIREN=block("storm_siren",EWBlocks.STORM_SIREN);
 public static final RegistryObject<Item> WEATHER_CONTROLLER=block("weather_controller",EWBlocks.WEATHER_CONTROLLER);
 public static final RegistryObject<Item> REINFORCED_GLASS=block("reinforced_glass",EWBlocks.REINFORCED_GLASS);
 private static RegistryObject<Item> block(String name,RegistryObject<net.minecraft.world.level.block.Block> block){return ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));}
 public static final RegistryObject<CreativeModeTab> TAB=CREATIVE_TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.extremeweather")).icon(()->WEATHER_RADAR.get().getDefaultInstance()).displayItems((params,output)->{output.accept(STORM_ANCHOR.get());output.accept(WEATHER_RADAR.get());output.accept(ANEMOMETER.get());output.accept(BAROMETER.get());output.accept(STORM_SIREN.get());output.accept(WEATHER_CONTROLLER.get());output.accept(REINFORCED_GLASS.get());}).build());
 private EWItems(){}
}
