package com.extremeweather.block;

import com.extremeweather.ExtremeWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nullved.pmweatherapi.storm.NearbyStorms;

public final class EWBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ExtremeWeather.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ExtremeWeather.MODID);
    public static final DeferredBlock<Block> SCORCHED_EARTH = BLOCKS.register("scorched_earth", () -> new Block(BlockBehaviour.Properties.of().strength(0.7f)));
    public static final DeferredBlock<Block> CHARRED_GRASS = BLOCKS.register("charred_grass", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f)));
    public static final DeferredBlock<Block> ASH = BLOCKS.register("ash", () -> new Block(BlockBehaviour.Properties.of().strength(0.2f)));
    public static final DeferredBlock<Block> WEATHER_RADAR = BLOCKS.register("weather_radar", () -> new RadarBlock());
    public static final DeferredBlock<Block> ANEMOMETER = BLOCKS.register("anemometer", () -> new InstrumentBlock("wind"));
    public static final DeferredBlock<Block> STORM_SIREN = BLOCKS.register("storm_siren", () -> new InstrumentBlock("siren"));
    public static final DeferredBlock<Block> BAROMETER = BLOCKS.register("barometer", () -> new InstrumentBlock("pressure"));
    static {
        ITEMS.registerSimpleBlockItem(SCORCHED_EARTH);
        ITEMS.registerSimpleBlockItem(CHARRED_GRASS);
        ITEMS.registerSimpleBlockItem(ASH);
        ITEMS.registerSimpleBlockItem(WEATHER_RADAR);
        ITEMS.registerSimpleBlockItem(ANEMOMETER);
        ITEMS.registerSimpleBlockItem(STORM_SIREN);
        ITEMS.registerSimpleBlockItem(BAROMETER);
    }
    private static class RadarBlock extends Block {
        RadarBlock() { super(BlockBehaviour.Properties.of().strength(3.0f)); }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                var storms = NearbyStorms.get(server).stormsNearBlock(pos, 512);
                player.displayClientMessage(storms.isEmpty()
                    ? Component.translatable("message.extremeweather.clear")
                    : Component.translatable("message.extremeweather.storms", storms.size()), true);
            }
            return InteractionResult.SUCCESS;
        }
    }
    private static class InstrumentBlock extends Block {
        private final String instrument;
        InstrumentBlock(String instrument) { super(BlockBehaviour.Properties.of().strength(2.5f));this.instrument=instrument; }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
            if(level instanceof ServerLevel server) {
                switch(instrument) {
                    case "wind" -> {
                        double speed=dev.protomanly.pmweather.weather.WindEngine.getWind(pos,server).horizontalDistance();
                        player.displayClientMessage(Component.translatable("message.extremeweather.wind",String.format(java.util.Locale.ROOT,"%.1f",speed)),true);
                    }
                    case "siren" -> {
                        boolean warning=!NearbyStorms.get(server).stormsNearBlock(pos,192).isEmpty();
                        player.displayClientMessage(Component.translatable(warning?"message.extremeweather.warning":"message.extremeweather.clear"),true);
                        if(warning)server.playSound(null,pos,net.minecraft.sounds.SoundEvents.BELL_BLOCK,net.minecraft.sounds.SoundSource.BLOCKS,2.0f,0.6f);
                    }
                    case "pressure" -> {
                        int count=NearbyStorms.get(server).stormsNearBlock(pos,256).size();
                        player.displayClientMessage(Component.translatable("message.extremeweather.pressure",1013-Math.min(90,count*25)),true);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
    }
    private EWBlocks() {}
}
