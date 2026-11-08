package com.extremeweather.command;

import com.extremeweather.disaster.DisasterEvents;
import com.extremeweather.fire.FireEvents;
import com.extremeweather.vortex.FunnelShape;
import com.extremeweather.compat.PMStormBridge;
import com.extremeweather.ocean.SurgeData;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.protomanly.pmweather.weather.storms.StormType;
import dev.protomanly.pmweather.weather.storms.StormTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.nullved.pmweatherapi.storm.StormBuilder;
import net.nullved.pmweatherapi.storm.NearbyStorms;
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
            .then(Commands.literal("help").executes(c->{c.getSource().sendSuccess(()->Component.literal("/ew storm <pm_type> [risk 0..100]; /ew vortex <shape> <ef 0..5> (native PM); /ew outline <shape> <ef> (addon preview); /ew disaster <name> [x y z]; /ew clear"),false);return 1;}))
            .then(Commands.literal("storm")
                .then(Commands.argument("type",StringArgumentType.word()).suggests((c,b)->suggest(b,stormNames()))
                    .executes(c->storm(c,50))
                    .then(Commands.argument("risk",IntegerArgumentType.integer(0,100)).executes(c->storm(c,IntegerArgumentType.getInteger(c,"risk"))))))
            .then(Commands.literal("vortex").then(Commands.argument("shape",StringArgumentType.word())
                .suggests((c,b)->suggest(b,Arrays.stream(FunnelShape.values()).map(v->v.name().toLowerCase(Locale.ROOT)).toArray(String[]::new)))
                .then(Commands.argument("ef",IntegerArgumentType.integer(0,5)).executes(EWCommands::vortex))))
            .then(Commands.literal("outline").then(Commands.argument("shape",StringArgumentType.word())
                .suggests((c,b)->suggest(b,Arrays.stream(FunnelShape.values()).map(v->v.name().toLowerCase(Locale.ROOT)).toArray(String[]::new)))
                .then(Commands.argument("ef",IntegerArgumentType.integer(0,5)).executes(EWCommands::outline))))
            .then(Commands.literal("ignite").then(Commands.argument("pos",Vec3Argument.vec3())
                .executes(c->{
                    BlockPos pos=BlockPos.containing(Vec3Argument.getVec3(c,"pos"));
                    if(!FireEvents.ignite(c.getSource().getLevel(),pos)){
                        c.getSource().sendFailure(Component.literal("Cannot ignite: no loaded fuel at "+pos.toShortString()));return 0;
                    }
                    c.getSource().sendSuccess(()->Component.literal("Wildfire started at "+pos.toShortString()),true);
                    return 1;
                })))
            .then(Commands.literal("firestatus").then(Commands.argument("pos",Vec3Argument.vec3())
                .executes(c->{
                    BlockPos pos=BlockPos.containing(Vec3Argument.getVec3(c,"pos"));
                    String status=FireEvents.status(c.getSource().getLevel(),pos);
                    c.getSource().sendSuccess(()->Component.literal(status),false);return 1;
                })))
            .then(Commands.literal("disaster").then(Commands.argument("name",StringArgumentType.word())
                .suggests((c,b)->suggest(b,EFFECTS)).executes(c->disaster(c,c.getSource().getPosition()))
                .then(Commands.argument("pos",Vec3Argument.vec3()).executes(c->disaster(c,Vec3Argument.getVec3(c,"pos"))))))
            .then(Commands.literal("status").executes(c->{
                ServerLevel level=c.getSource().getLevel();
                int count=NearbyStorms.get(level).stormsNearBlock(BlockPos.containing(c.getSource().getPosition()),4096).size();
                int fires=FireEvents.active(level);
                c.getSource().sendSuccess(()->Component.literal("PMWeather storms nearby: "+count+"; tracked wildfire blocks: "+fires),false);
                return 1;
            }))
            .then(Commands.literal("clear").executes(c->{DisasterEvents.get(c.getSource().getLevel()).clearManual();c.getSource().sendSuccess(()->Component.literal("Addon outlines cleared; PMWeather storms unaffected. Use /ew clear all for a complete reset."),true);return 1;})
                .then(Commands.literal("all").executes(c->{
                    ServerLevel level=c.getSource().getLevel();
                    DisasterEvents.get(level).clearAll();
                    FireEvents.stop(level);
                    SurgeData.get(level).cleanupAll(level);
                    var handler=GameBusEvents.MANAGERS.get(level.dimension());
                    if(handler instanceof WeatherHandlerServer server)server.clearAllStorms();
                    c.getSource().sendSuccess(()->Component.literal("PMWeather storms and addon weather cleared in this dimension."),true);
                    return 1;
                }))));
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
    private static FunnelShape requestedShape(CommandContext<CommandSourceStack> context) {
        return FunnelShape.parse(StringArgumentType.getString(context,"shape"));
    }
    private static int vortex(CommandContext<CommandSourceStack> context) {
        FunnelShape shape=requestedShape(context);
        if(shape==null){context.getSource().sendFailure(Component.literal("Unknown shape. Use tab completion."));return 0;}
        int ef=IntegerArgumentType.getInteger(context,"ef");ServerLevel level=context.getSource().getLevel();
        try {
            if(PMStormBridge.nativeFunnel(level,context.getSource().getPosition(),shape,ef)==null){
                context.getSource().sendFailure(Component.literal("PMWeather cannot spawn here (unloaded chunk or storm limit)."));return 0;
            }
        } catch(RuntimeException error){context.getSource().sendFailure(Component.literal("PMWeather spawn failed: "+error.getMessage()));return 0;}
        context.getSource().sendSuccess(()->Component.literal("Native PMWeather supercell EF"+ef+"; approximate "+shape+" profile. PMWeather controls the actual funnel."),true);
        return 1;
    }
    private static int outline(CommandContext<CommandSourceStack> context) {
        FunnelShape shape=requestedShape(context);
        if(shape==null){context.getSource().sendFailure(Component.literal("Unknown shape. Use tab completion."));return 0;}
        int ef=IntegerArgumentType.getInteger(context,"ef");ServerLevel level=context.getSource().getLevel();
        if(!DisasterEvents.get(level).summonFunnel(level,context.getSource().getPosition(),shape,ef)){
            context.getSource().sendFailure(Component.literal("Maximum of four addon outlines per dimension or unloaded chunk."));return 0;
        }
        context.getSource().sendSuccess(()->Component.literal("Addon particle outline "+shape+" EF"+ef+" (NOT native PMWeather)."),true);return 1;
    }
    private static int disaster(CommandContext<CommandSourceStack> context,net.minecraft.world.phys.Vec3 position) {
        String name=StringArgumentType.getString(context,"name");
        if(Arrays.stream(EFFECTS).noneMatch(name::equals)){
            context.getSource().sendFailure(Component.literal("Unknown disaster. Use tab completion."));return 0;
        }
        ServerLevel level=context.getSource().getLevel();
        if(name.equals("wildfire") || name.equals("forest_fire")) {
            BlockPos ground=BlockPos.containing(position);
            if(!FireEvents.ignite(level,ground)){
                context.getSource().sendFailure(Component.literal("Wildfire needs a loaded grass/dirt/tree block. Aim at fuel, not empty air."));return 0;
            }
            context.getSource().sendSuccess(()->Component.literal("Wildfire started at "+ground.toShortString()),true);return 1;
        }
        var result=DisasterEvents.get(level).trigger(level,position,name);
        if(!result.started()){context.getSource().sendFailure(Component.literal(result.reason()));return 0;}
        context.getSource().sendSuccess(()->Component.literal(result.reason()),true);return 1;
    }
}
