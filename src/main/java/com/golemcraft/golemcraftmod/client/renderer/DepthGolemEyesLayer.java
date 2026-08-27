package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class DepthGolemEyesLayer extends EyesLayer<BaseGolemRenderState, com.golemcraft.golemcraftmod.client.model.BaseGolemModel> {
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/depth_golem_glow.png"));

    public DepthGolemEyesLayer(RenderLayerParent<BaseGolemRenderState, com.golemcraft.golemcraftmod.client.model.BaseGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }

    @Override
    public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        float time = state.ageInTicks; // simplified
        float alpha = (float) (Math.cos(time * 0.1F) * 0.4F + 0.6F); // pulsates between 0.2 and 1.0
        
        // Boost brightness when charging/attacking
        if (state.attackAnimProgress > 0.0F || state.isAggressive) {
            // Flash fully bright when swinging, or pulse faster/brighter when aggressive
            float aggressivePulse = (float) (Math.cos(time * 0.3F) * 0.2F + 0.8F);
            alpha = Math.max(alpha, aggressivePulse);
            
            if (state.attackAnimProgress > 0.0F) {
                alpha = 1.0F; // Maximum brightness during the strike
            }
        }
        
        int r = (int) (255.0F * alpha);
        int g = (int) (255.0F * alpha);
        int b = (int) (255.0F * alpha);
        int argb = (255 << 24) | (r << 16) | (g << 8) | b; // Alpha is always 255, RGB scales down
        
        submitNodeCollector.submitModel(this.getParentModel(), state, poseStack, this.renderType(), 15728640, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, argb, null);
    }
}
