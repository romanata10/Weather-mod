package com.extremeweather.fire;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.Map;
import java.util.WeakHashMap;

public final class FireEvents {
    private final Map<ServerLevel,WildfireManager> managers=new WeakHashMap<>();
    @SubscribeEvent public void tick(LevelTickEvent.Post event) {
        if(event.getLevel() instanceof ServerLevel level) managers.computeIfAbsent(level,k->new WildfireManager()).tick(level);
    }
    @SubscribeEvent public void strike(EntityJoinLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel level && event.getEntity().getType()==EntityType.LIGHTNING_BOLT && level.random.nextFloat()<.4f)
            managers.computeIfAbsent(level,k->new WildfireManager()).ignite(level,event.getEntity().blockPosition().below());
    }
}
