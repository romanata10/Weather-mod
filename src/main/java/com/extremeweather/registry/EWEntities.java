package com.extremeweather.registry;
import com.extremeweather.ExtremeWeather;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
public final class EWEntities {
 public static final DeferredRegister<EntityType<?>> ENTITY_TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ExtremeWeather.MODID);
 private EWEntities(){}
}
