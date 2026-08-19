package com.trycraft.trycraftmod.events;

import com.trycraft.trycraftmod.TryCraft;
import com.trycraft.trycraftmod.client.model.FlowerGolemModel;
import com.trycraft.trycraftmod.client.renderer.FlowerGolemRenderer;
import com.trycraft.trycraftmod.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TryCraft.MODID, value = Dist.CLIENT)
public class ClientEvents {
    
    public static final ModelLayerLocation FLOWER_GOLEM_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(TryCraft.MODID, "flower_golem"), "main");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FLOWER_GOLEM.get(), FlowerGolemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FLOWER_GOLEM_LAYER, FlowerGolemModel::createBodyLayer);
    }
    
    public static class ModelLayers {}
}
