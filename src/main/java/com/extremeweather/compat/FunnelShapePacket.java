package com.extremeweather.compat;

import com.extremeweather.ExtremeWeather;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Only a native storm ID and its current continuous taper; no second funnel. */
public record FunnelShapePacket(long stormId,float taper) implements CustomPacketPayload {
    public static final Type<FunnelShapePacket> TYPE=new Type<>(
        ResourceLocation.fromNamespaceAndPath(ExtremeWeather.MODID,"native_funnel_taper"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FunnelShapePacket> CODEC=
        StreamCodec.of((buffer,payload)->{
            buffer.writeLong(payload.stormId());buffer.writeFloat(payload.taper());
        },buffer->new FunnelShapePacket(buffer.readLong(),buffer.readFloat()));
    @Override public Type<FunnelShapePacket> type(){return TYPE;}

    @EventBusSubscriber(modid=ExtremeWeather.MODID,bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->
                context.enqueueWork(()->com.extremeweather.client.NativeShapeOverride.receive(payload)));
        }
    }
}
