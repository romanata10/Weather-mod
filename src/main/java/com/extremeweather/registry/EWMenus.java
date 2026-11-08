package com.extremeweather.registry;
import com.extremeweather.ExtremeWeather;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
public final class EWMenus {
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ExtremeWeather.MODID);
 private EWMenus(){}
}
