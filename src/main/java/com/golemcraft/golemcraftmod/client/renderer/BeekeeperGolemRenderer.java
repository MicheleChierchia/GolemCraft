package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import com.golemcraft.golemcraftmod.entity.BeekeeperGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class BeekeeperGolemRenderer extends BaseGolemRenderer {
    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/beekeeper_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_beekeeper_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_beekeeper_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_beekeeper_golem.png")
    };

    public BeekeeperGolemRenderer(EntityRendererProvider.Context context) {
        super(context, com.golemcraft.golemcraftmod.events.ClientEvents.BEEKEEPER_GOLEM_LAYER);
        this.addLayer(new BeekeeperGolemEyesLayer(this));
        this.addLayer(new BeekeeperHoneyLayer(this, this.getModel()));
    }

    @Override
    public void extractRenderState(BaseGolemEntity entity, BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof BeekeeperGolemEntity beekeeper) {
            state.honeyLevel = beekeeper.getHoneyLevel();
            state.isBeekeeperGolem = true;
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
