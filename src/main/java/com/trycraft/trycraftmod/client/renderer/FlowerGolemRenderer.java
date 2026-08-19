package com.trycraft.trycraftmod.client.renderer;

import com.trycraft.trycraftmod.TryCraft;
import com.trycraft.trycraftmod.client.model.FlowerGolemModel;
import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FlowerGolemRenderer extends MobRenderer<FlowerGolemEntity, FlowerGolemRenderState, FlowerGolemModel> {
    private static final Identifier GOLEM_LOCATION = Identifier.fromNamespaceAndPath(TryCraft.MODID, "textures/entity/flower_golem.png");

    public FlowerGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new FlowerGolemModel(context.bakeLayer(com.trycraft.trycraftmod.events.ClientEvents.FLOWER_GOLEM_LAYER)), 0.5F);
        // this.addLayer(new FlowerGolemEyesLayer(this));
    }

    @Override
    public FlowerGolemRenderState createRenderState() {
        return new FlowerGolemRenderState();
    }

    @Override
    public void extractRenderState(FlowerGolemEntity entity, FlowerGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }

    @Override
    public Identifier getTextureLocation(FlowerGolemRenderState state) {
        return GOLEM_LOCATION;
    }
}
