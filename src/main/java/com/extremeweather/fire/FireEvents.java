package com.extremeweather.fire;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.Map;
import java.util.WeakHashMap;

public final class FireEvents {
    private static final Map<ServerLevel,WildfireManager> managers=new WeakHashMap<>();
    public static String status(ServerLevel level,net.minecraft.core.BlockPos pos) {
        return managers.computeIfAbsent(level,k->new WildfireManager()).describe(level,pos);
    }
    public static void ignite(ServerLevel level,net.minecraft.core.BlockPos pos){managers.computeIfAbsent(level,k->new WildfireManager()).ignite(level,pos);}
    @SubscribeEvent public void tick(LevelTickEvent.Post event) {
        if(event.getLevel() instanceof ServerLevel level) {
            WildfireManager manager=managers.computeIfAbsent(level,k->new WildfireManager());
            if(level.getGameTime()%40==0)
                for(var player:level.players())manager.seedNearby(level,player.blockPosition());
            manager.tick(level);
        }
    }
    @SubscribeEvent public void strike(EntityJoinLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel level && event.getEntity().getType()==EntityType.LIGHTNING_BOLT && level.random.nextFloat()<.4f)
            managers.computeIfAbsent(level,k->new WildfireManager()).ignite(level,event.getEntity().blockPosition().below());
    }
}
