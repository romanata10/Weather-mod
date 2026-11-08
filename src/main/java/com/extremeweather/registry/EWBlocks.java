package com.extremeweather.registry;

import com.extremeweather.ExtremeWeather;
import com.extremeweather.weather.WeatherSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.material.MapColor;

public final class EWBlocks {
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ExtremeWeather.MODID);
 public static final RegistryObject<Block> STORM_ANCHOR=BLOCKS.register("storm_anchor",()->new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(8,1200).sound(SoundType.METAL)));
 public static final RegistryObject<Block> WEATHER_RADAR=BLOCKS.register("weather_radar",()->new InstrumentBlock("radar"));
 public static final RegistryObject<Block> ANEMOMETER=BLOCKS.register("anemometer",()->new InstrumentBlock("wind"));
 public static final RegistryObject<Block> BAROMETER=BLOCKS.register("barometer",()->new InstrumentBlock("pressure"));
 public static final RegistryObject<Block> STORM_SIREN=BLOCKS.register("storm_siren",()->new InstrumentBlock("siren"));
 public static final RegistryObject<Block> WEATHER_CONTROLLER=BLOCKS.register("weather_controller",()->new InstrumentBlock("controller"));
 public static final RegistryObject<Block> REINFORCED_GLASS=BLOCKS.register("reinforced_glass",()->new net.minecraft.world.level.block.GlassBlock(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(6,60).noOcclusion().sound(SoundType.GLASS)));
 private static class InstrumentBlock extends Block {
  private final String function;
  InstrumentBlock(String function){super(Block.Properties.of().mapColor(MapColor.METAL).strength(3,6).sound(SoundType.METAL));this.function=function;}
  @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
   if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
   var engine=WeatherSavedData.get(server).engine;
   var c=engine.nearest(pos.getX(),pos.getZ());
   switch(function){
    case "radar" -> player.displayClientMessage(c==null?Component.translatable("message.extremeweather.clear"):Component.translatable("message.extremeweather.radar",c.type.displayName(),(int)Math.hypot(c.x-pos.getX(),c.z-pos.getZ())),true);
    case "wind" -> player.displayClientMessage(Component.translatable("message.extremeweather.wind",String.format("%.2f",engine.windAt(pos.getX(),pos.getZ()).horizontalDistance())),true);
    case "pressure" -> player.displayClientMessage(Component.translatable("message.extremeweather.pressure",c==null?1013:Math.round(1013-55*c.intensity)),true);
    case "siren" -> player.displayClientMessage(Component.translatable(c!=null&&c.contains(pos.getX(),pos.getZ())?"message.extremeweather.warning":"message.extremeweather.clear"),true);
    case "controller" -> {
     if(!player.hasPermissions(2)){player.displayClientMessage(Component.translatable("message.extremeweather.permission"),true);break;}
     if(player.isShiftKeyDown()){engine.clear(server);player.displayClientMessage(Component.translatable("message.extremeweather.clear"),true);}
     else {engine.spawnCell(server,com.extremeweather.weather.CellType.SUPERCELL,pos);player.displayClientMessage(Component.translatable("message.extremeweather.spawned"),true);}
    }
   }
   return InteractionResult.CONSUME;
  }
 }
 private EWBlocks(){}
}
