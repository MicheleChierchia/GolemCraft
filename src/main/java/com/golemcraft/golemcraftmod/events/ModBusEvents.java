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
        event.put(ModEntities.FISHERMAN_GOLEM.get(), com.golemcraft.golemcraftmod.entity.FishermanGolemEntity.createAttributes().build());
        event.put(ModEntities.SOLDIER_GOLEM.get(), com.golemcraft.golemcraftmod.entity.SoldierGolemEntity.createAttributes().build());
        event.put(ModEntities.LUMBERJACK_GOLEM.get(), com.golemcraft.golemcraftmod.entity.LumberjackGolemEntity.createAttributes().build());
        event.put(ModEntities.DEPTH_GOLEM.get(), com.golemcraft.golemcraftmod.entity.DepthGolemEntity.createAttributes().build());
        event.put(ModEntities.EXPLORER_GOLEM.get(), com.golemcraft.golemcraftmod.entity.ExplorerGolemEntity.createAttributes().build());
    }
}
