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

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FLOWER_GOLEM.get(), FlowerGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.BASE_GOLEM.get(), com.golemcraft.golemcraftmod.client.renderer.BaseGolemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FLOWER_GOLEM_LAYER, com.golemcraft.golemcraftmod.client.model.BaseGolemModel::createBodyLayer);
    }
    
    public static class ModelLayers {}
}
