package com.extremeweather.registry;
import com.extremeweather.ExtremeWeather;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
public final class EWBlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ExtremeWeather.MODID);
 private EWBlockEntities(){}
}
