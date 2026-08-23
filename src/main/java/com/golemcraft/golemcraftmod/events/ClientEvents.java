package com.golemcraft.golemcraftmod.events;

import com.golemcraft.golemcraftmod.GolemCraft;

import com.golemcraft.golemcraftmod.client.renderer.FlowerGolemRenderer;
import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = GolemCraft.MODID, value = Dist.CLIENT)
public class ClientEvents {
    
    public static final ModelLayerLocation FLOWER_GOLEM_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "flower_golem"), "main");
    public static final ModelLayerLocation FARMER_GOLEM_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "farmer_golem"), "main");
    public static final ModelLayerLocation FISHERMAN_GOLEM_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "fisherman_golem"), "main");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FLOWER_GOLEM.get(), FlowerGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.BASE_GOLEM.get(), com.golemcraft.golemcraftmod.client.renderer.BaseGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.FARMER_GOLEM.get(), com.golemcraft.golemcraftmod.client.renderer.FarmerGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.FISHERMAN_GOLEM.get(), com.golemcraft.golemcraftmod.client.renderer.FishermanGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.SOLDIER_GOLEM.get(), com.golemcraft.golemcraftmod.client.renderer.SoldierGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLEM_FISHING_HOOK.get(), com.golemcraft.golemcraftmod.client.renderer.GolemFishingHookRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FLOWER_GOLEM_LAYER, com.golemcraft.golemcraftmod.client.model.BaseGolemModel::createBodyLayer);
        event.registerLayerDefinition(FARMER_GOLEM_LAYER, com.golemcraft.golemcraftmod.client.model.BaseGolemModel::createFarmerBodyLayer);
        event.registerLayerDefinition(FISHERMAN_GOLEM_LAYER, com.golemcraft.golemcraftmod.client.model.BaseGolemModel::createFishermanBodyLayer);
    }
    
    public static class ModelLayers {}
}
