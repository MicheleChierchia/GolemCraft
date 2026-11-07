package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import com.golemcraft.golemcraftmod.entity.FishermanGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class FishermanGolemRenderer extends BaseGolemRenderer {
    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/fisherman_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_fisherman_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_fisherman_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_fisherman_golem.png")
    };

    public FishermanGolemRenderer(EntityRendererProvider.Context context) {
        super(context, com.golemcraft.golemcraftmod.events.ClientEvents.FISHERMAN_GOLEM_LAYER);
        this.addLayer(new GolemEyesLayer(this, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/flower_golem_eyes.png")));
    }

    @Override
    public void extractRenderState(BaseGolemEntity entity, BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        if (entity instanceof FishermanGolemEntity fisherman) {
            state.isFishing = fisherman.isFishing();
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
