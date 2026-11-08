package com.extremeweather.event;

import com.extremeweather.weather.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EWCommands {
 @SubscribeEvent public void register(RegisterCommandsEvent event) {
  CommandDispatcher<CommandSourceStack> dispatcher=event.getDispatcher();
  var root=Commands.literal("ew").requires(source->source.hasPermission(2));
  root.then(Commands.literal("status").executes(ctx->{
   ServerLevel level=ctx.getSource().getLevel();var engine=WeatherSavedData.get(level).engine;
   ctx.getSource().sendSuccess(()->Component.literal("Weather: "+engine.cells().size()+" cells, "+engine.vortices().size()+" vortices"),false);return 1;
  }));
  root.then(Commands.literal("clear").executes(ctx->{WeatherSavedData.get(ctx.getSource().getLevel()).engine.clear(ctx.getSource().getLevel());ctx.getSource().sendSuccess(()->Component.literal("Extreme Weather cleared"),true);return 1;}));
  var storm=Commands.literal("storm");
  for(CellType type:CellType.values()){
   storm.then(Commands.literal(type.name().toLowerCase()).executes(ctx->spawnCell(ctx.getSource(),type,BlockPos.containing(ctx.getSource().getPosition())))
    .then(Commands.argument("pos",Vec3Argument.vec3()).executes(ctx->spawnCell(ctx.getSource(),type,BlockPos.containing(Vec3Argument.getVec3(ctx,"pos"))))));
  }
  root.then(storm);
  var vortex=Commands.literal("vortex");
  for(VortexType type:VortexType.values()){
   vortex.then(Commands.literal(type.name().toLowerCase()).executes(ctx->spawnVortex(ctx.getSource(),type,1,BlockPos.containing(ctx.getSource().getPosition())))
    .then(Commands.argument("ef",FloatArgumentType.floatArg(0,5)).executes(ctx->spawnVortex(ctx.getSource(),type,FloatArgumentType.getFloat(ctx,"ef"),BlockPos.containing(ctx.getSource().getPosition())))
     .then(Commands.argument("pos",Vec3Argument.vec3()).executes(ctx->spawnVortex(ctx.getSource(),type,FloatArgumentType.getFloat(ctx,"ef"),BlockPos.containing(Vec3Argument.getVec3(ctx,"pos")))))));
  }
  root.then(vortex);dispatcher.register(root);
 }
 private static int spawnCell(CommandSourceStack source,CellType type,BlockPos pos){WeatherSavedData.get(source.getLevel()).engine.spawnCell(source.getLevel(),type,pos);source.sendSuccess(()->Component.literal("Spawned "+type.name()+" at "+pos.toShortString()),true);return 1;}
 private static int spawnVortex(CommandSourceStack source,VortexType type,float ef,BlockPos pos){WeatherSavedData.get(source.getLevel()).engine.spawnVortex(source.getLevel(),type,pos,ef);source.sendSuccess(()->Component.literal("Spawned "+type.name()+" EF"+ef+" at "+pos.toShortString()),true);return 1;}
}
