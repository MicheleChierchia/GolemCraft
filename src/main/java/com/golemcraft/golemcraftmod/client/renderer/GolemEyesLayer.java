package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.function.Predicate;

public class GolemEyesLayer extends EyesLayer<BaseGolemRenderState, BaseGolemModel> {
    private final RenderType eyesRenderType;
    private final Predicate<BaseGolemRenderState> canRender;

    public GolemEyesLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer, Identifier textureLocation) {
        this(renderer, textureLocation, state -> state.oxidationLevel < 3);
    }

    public GolemEyesLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer, Identifier textureLocation, Predicate<BaseGolemRenderState> canRender) {
        super(renderer);
        this.eyesRenderType = RenderTypes.eyes(textureLocation);
        this.canRender = canRender;
    }

    @Override
    public RenderType renderType() {
        return this.eyesRenderType;
    }

    @Override
    public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        if (this.canRender.test(state)) {
            super.submit(poseStack, submitNodeCollector, packedLight, state, limbSwing, limbSwingAmount);
        }
    }
}
