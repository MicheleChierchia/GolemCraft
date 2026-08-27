package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.DepthGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class DepthGolemRenderer extends BaseGolemRenderer {
    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/depth_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_depth_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_depth_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_depth_golem.png")
    };

    public DepthGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new com.golemcraft.golemcraftmod.client.model.DepthGolemModel(context.bakeLayer(com.golemcraft.golemcraftmod.events.ClientEvents.DEPTH_GOLEM_LAYER)));
        this.addLayer(new DepthGolemEyesLayer(this));
    }

    @Override
    public void extractRenderState(com.golemcraft.golemcraftmod.entity.BaseGolemEntity entity,
                                   BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof DepthGolemEntity depthGolem) {
            state.isGuarding = depthGolem.isGuarding();
            state.isDepthGolem = true;
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
