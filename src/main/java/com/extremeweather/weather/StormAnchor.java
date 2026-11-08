package com.extremeweather.weather;

import com.extremeweather.EWConfig;
import com.extremeweather.registry.EWBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class StormAnchor {
    private StormAnchor() {}
    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        int radius = EWConfig.STORM_ANCHOR_RADIUS.get();
        // Check only the local area; tornado block destruction calls this frequently.
        for (int x = -radius; x <= radius; x += 8)
            for (int z = -radius; z <= radius; z += 8)
                for (int y = -16; y <= 16; y += 8) {
                    BlockPos at = pos.offset(x, y, z);
                    if (level.hasChunkAt(at) && level.getBlockState(at).is(EWBlocks.STORM_ANCHOR.get())) return true;
                }
        return false;
    }
}
