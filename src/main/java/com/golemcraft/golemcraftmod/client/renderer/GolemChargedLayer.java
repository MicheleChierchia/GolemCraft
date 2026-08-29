package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.Identifier;

public class GolemChargedLayer extends EnergySwirlLayer<BaseGolemRenderState, BaseGolemModel> {
    private static final Identifier POWER_LOCATION = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private final BaseGolemModel model;

    public GolemChargedLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer, BaseGolemModel model) {
        super(renderer);
        this.model = model;
    }

    @Override
    protected boolean isPowered(BaseGolemRenderState state) {
        return state.isCharged;
    }

    @Override
    protected float xOffset(float ageInTicks) {
        return ageInTicks * 0.01F;
    }

    @Override
    protected Identifier getTextureLocation() {
        return POWER_LOCATION;
    }

    @Override
    protected BaseGolemModel model() {
        return this.model;
    }
}

