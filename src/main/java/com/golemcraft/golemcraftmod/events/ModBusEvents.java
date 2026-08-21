package com.golemcraft.golemcraftmod.events;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.FlowerGolemEntity;
import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = GolemCraft.MODID)
public class ModBusEvents {
    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FLOWER_GOLEM.get(), FlowerGolemEntity.createAttributes().build());
        event.put(ModEntities.BASE_GOLEM.get(), com.golemcraft.golemcraftmod.entity.BaseGolemEntity.createAttributes().build());
        event.put(ModEntities.FARMER_GOLEM.get(), com.golemcraft.golemcraftmod.entity.FarmerGolemEntity.createAttributes().build());
        event.put(ModEntities.SOLDIER_GOLEM.get(), com.golemcraft.golemcraftmod.entity.SoldierGolemEntity.createAttributes().build());
    }
}
