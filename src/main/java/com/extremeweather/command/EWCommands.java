package com.extremeweather.command;

import com.extremeweather.disaster.DisasterEvents;
import com.extremeweather.fire.FireEvents;
import com.extremeweather.vortex.FunnelShape;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.protomanly.pmweather.weather.storms.StormType;
import dev.protomanly.pmweather.weather.storms.StormTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.nullved.pmweatherapi.storm.StormBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Operator commands. PMWeather storms are created via the public PMWeatherAPI StormBuilder. */
public final class EWCommands {
    private static final String[] EFFECTS={"hail","rainbow","sprite","ball_lightning","superbolt","squall","dust_devil","waterspout","landspout","snow_whirl","fire_whirl","hurricane","typhoon","cyclone","meteotsunami","tsunami","surge","flood","wildfire","forest_fire"};
    @SubscribeEvent public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ew").requires(s->s.hasPermission(2))
            .then(Commands.literal("help").executes(c->{c.getSource().sendSuccess(()->Component.literal("/ew storm <pm_type> [risk 0..100]; /ew vortex <shape> <ef 0..5>; /ew disaster <name>; /ew clear"),false);return 1;}))
            .then(Commands.literal("storm")
                .then(Commands.argument("type",StringArgumentType.word()).suggests((c,b)->suggest(b,stormNames()))
                    .executes(c->storm(c,50))
                    .then(Commands.argument("risk",IntegerArgumentType.integer(0,100)).executes(c->storm(c,IntegerArgumentType.getInteger(c,"risk"))))))
            .then(Commands.literal("vortex").then(Commands.argument("shape",StringArgumentType.word())
                .suggests((c,b)->suggest(b,Arrays.stream(FunnelShape.values()).map(v->v.name().toLowerCase(Locale.ROOT)).toArray(String[]::new)))
                .then(Commands.argument("ef",IntegerArgumentType.integer(0,5)).executes(EWCommands::vortex))))
            .then(Commands.literal("disaster").then(Commands.argument("name",StringArgumentType.word())
                .suggests((c,b)->suggest(b,EFFECTS)).executes(EWCommands::disaster)))
            .then(Commands.literal("clear").executes(c->{DisasterEvents.get(c.getSource().getLevel()).clearManual();c.getSource().sendSuccess(()->Component.literal("Addon command-spawned vortices cleared (PMWeather storms unaffected)."),true);return 1;})));
    }
    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder,String[] values) {
        for(String value:values)if(value.startsWith(builder.getRemainingLowerCase()))builder.suggest(value);
        return builder.buildFuture();
    }
    private static String[] stormNames() {
        return Arrays.stream(StormTypes.class.getFields()).filter(f->Modifier.isStatic(f.getModifiers())&&StormType.class.isAssignableFrom(f.getType()))
            .map(f->f.getName().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    }
    private static StormType nativeType(String name) {
        for(Field f:StormTypes.class.getFields()) {
            if(!f.getName().equalsIgnoreCase(name) || !Modifier.isStatic(f.getModifiers()) || !StormType.class.isAssignableFrom(f.getType()))continue;
            try{return (StormType)f.get(null);}catch(IllegalAccessException ignored){return null;}
        }
        return null;
    }
    private static int storm(CommandContext<CommandSourceStack> context,int risk) {
        String name=StringArgumentType.getString(context,"type");StormType type=nativeType(name);
        if(type==null){context.getSource().sendFailure(Component.literal("Unknown PMWeather storm type. Use tab completion."));return 0;}
        try {
            ServerLevel level=context.getSource().getLevel();
            new StormBuilder(level,type,context.getSource().getPosition()).risk(risk/100f).buildAndSpawn();
            context.getSource().sendSuccess(()->Component.literal("Spawned PMWeather storm: "+name),true);return 1;
        } catch(RuntimeException e){context.getSource().sendFailure(Component.literal("PMWeather rejected storm: "+e.getMessage()));return 0;}
    }
    private static int vortex(CommandContext<CommandSourceStack> context) {
        String name=StringArgumentType.getString(context,"shape");FunnelShape shape=FunnelShape.parse(name);
        if(shape==null){context.getSource().sendFailure(Component.literal("Unknown shape. Use tab completion."));return 0;}
        int ef=IntegerArgumentType.getInteger(context,"ef");ServerLevel level=context.getSource().getLevel();
        if(!DisasterEvents.get(level).summonFunnel(level,context.getSource().getPosition(),shape,ef)){
            context.getSource().sendFailure(Component.literal("Maximum of four command-spawned vortices per dimension, or chunk not loaded."));return 0;
        }
        context.getSource().sendSuccess(()->Component.literal("Addon funnel "+shape+" EF"+ef+" (particle silhouette, NOT PMWeather renderer)"),true);return 1;
    }
    private static int disaster(CommandContext<CommandSourceStack> context) {
        String name=StringArgumentType.getString(context,"name");
        if(Arrays.stream(EFFECTS).noneMatch(name::equals)){context.getSource().sendFailure(Component.literal("Unknown disaster. Use tab completion."));return 0;}
        try {
            ServerPlayer player=context.getSource().getPlayerOrException();
            if(name.equals("wildfire") || name.equals("forest_fire"))FireEvents.ignite(player.serverLevel(),player.blockPosition().below());
            else DisasterEvents.get(player.serverLevel()).summonEffect(player.serverLevel(),player,name);
            context.getSource().sendSuccess(()->Component.literal("Triggered "+name),true);return 1;
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){context.getSource().sendFailure(Component.literal("This command requires an operator player."));return 0;}
    }
}
