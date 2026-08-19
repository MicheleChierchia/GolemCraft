package com.trycraft.trycraftmod.events;

import com.trycraft.trycraftmod.TryCraft;
import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
import com.trycraft.trycraftmod.registry.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = TryCraft.MODID)
public class ModBusEvents {
    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FLOWER_GOLEM.get(), FlowerGolemEntity.createAttributes().build());
    }
}
