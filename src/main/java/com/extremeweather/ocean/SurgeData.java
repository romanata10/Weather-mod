package com.extremeweather.ocean;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.tags.FluidTags;
import java.util.*;

/** Tracks temporary flood water across restarts so a storm cannot leave permanent source blocks. */
public final class SurgeData extends SavedData {
    private final Map<BlockPos,Long> placed=new HashMap<>();
    public static SurgeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(SurgeData::new,SurgeData::load),"extremeweather_surge");
    }
    private static SurgeData load(CompoundTag tag, HolderLookup.Provider registries) {
        SurgeData data=new SurgeData();
        for(var entry:tag.getList("Surge",10)) {
            CompoundTag value=(CompoundTag)entry;
            data.placed.put(BlockPos.of(value.getLong("Position")),value.getLong("Expires"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        ListTag list=new ListTag();
        for(var entry:placed.entrySet()) {
            CompoundTag value=new CompoundTag();
            value.putLong("Position",entry.getKey().asLong());
            value.putLong("Expires",entry.getValue());
            list.add(value);
        }
        tag.put("Surge",list);return tag;
    }
    public void place(ServerLevel level,BlockPos pos,int duration) {
        if(placed.size()>=128 || !level.hasChunkAt(pos) || !level.getBlockState(pos).isAir())return;
        // Only temporary crests above existing water. Never overwrite structures or terrain.
        if(!level.getFluidState(pos.below()).is(FluidTags.WATER))return;
        placed.put(pos,level.getGameTime()+duration);
        level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);
        setDirty();
    }
    public void tick(ServerLevel level) {
        Iterator<Map.Entry<BlockPos,Long>> it=placed.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();BlockPos pos=entry.getKey();
            if(level.getGameTime()<entry.getValue() || !level.hasChunkAt(pos))continue;
            if(level.getBlockState(pos).is(Blocks.WATER))level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            it.remove();setDirty();
        }
    }
}
