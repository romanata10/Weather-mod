package com.extremeweather.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.nullved.pmweatherapi.storm.NearbyStorms;

/** Automatically polls PMWeatherAPI and exposes an alarm signal to redstone. */
public final class StormSirenBlock extends Block {
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
    public StormSirenBlock() {
        super(BlockBehaviour.Properties.of().strength(2.5f));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(ACTIVE);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving) {
        super.onPlace(state,level,pos,previous,moving);
        if(!level.isClientSide)level.scheduleTick(pos,this,20);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        boolean danger=!NearbyStorms.get(level).stormsNearBlock(pos,192).isEmpty();
        if(danger!=state.getValue(ACTIVE)) {
            level.setBlock(pos,state.setValue(ACTIVE,danger),3);
            level.updateNeighborsAt(pos,this);
            if(danger)level.playSound(null,pos,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,2.0f,.6f);
        } else if(danger && level.getGameTime()%100<20) {
            level.playSound(null,pos,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,1.4f,.6f);
        }
        level.scheduleTick(pos,this,20);
    }
    @Override protected boolean isSignalSource(BlockState state) {return true;}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) {return state.getValue(ACTIVE)?15:0;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide)player.displayClientMessage(Component.translatable(state.getValue(ACTIVE)?"message.extremeweather.warning":"message.extremeweather.clear"),true);
        return InteractionResult.SUCCESS;
    }
}
