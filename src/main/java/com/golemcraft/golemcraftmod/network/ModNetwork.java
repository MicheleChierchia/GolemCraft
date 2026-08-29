package com.golemcraft.golemcraftmod.network;

import com.golemcraft.golemcraftmod.GolemCraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GolemCraft.MODID)
public class ModNetwork {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(GolemCraft.MODID).versioned("1.0");
        registrar.playToServer(
                GolemBeaconUpdatePacket.TYPE,
                GolemBeaconUpdatePacket.STREAM_CODEC,
                GolemBeaconUpdatePacket::handle
        );
    }
}
