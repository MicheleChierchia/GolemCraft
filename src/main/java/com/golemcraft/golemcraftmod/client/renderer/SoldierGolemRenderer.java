package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.SoldierGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class SoldierGolemRenderer extends BaseGolemRenderer {
    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/soldier_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_soldier_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_soldier_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_soldier_golem.png")
    };

    public SoldierGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new FlowerGolemEyesLayer(this));
    }

    @Override
    public void extractRenderState(com.golemcraft.golemcraftmod.entity.BaseGolemEntity entity,
                                   BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof SoldierGolemEntity soldier) {
            int ticks = soldier.getAttackAnimTicks();
            // Convert ticks (10→0) into a 0→1→0 bell-curve progress
            state.attackAnimProgress = ticks > 0 ? ticks / 10.0f : 0.0f;
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
