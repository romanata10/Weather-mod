package com.extremeweather.client;

import com.extremeweather.ExtremeWeather;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Original GPU density shader, loaded only on the client and on resource reload.
 * Registration alone does not draw any geometry: callers must supply a bounded
 * quad at a synchronized weather position and test it in the Minecraft client. */
@EventBusSubscriber(modid=ExtremeWeather.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class VolumeShader {
    private static ShaderInstance vortex;
    private VolumeShader() {}
    public static ShaderInstance vortex(){return vortex;}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            ResourceLocation.fromNamespaceAndPath(ExtremeWeather.MODID,"vortex_volume"),
            DefaultVertexFormat.POSITION_TEX_COLOR), loaded->vortex=loaded);
    }
}
