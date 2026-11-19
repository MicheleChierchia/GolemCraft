package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;

public class AlchemistPotionLayer extends RenderLayer<BaseGolemRenderState, BaseGolemModel> {

    public AlchemistPotionLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        if (state.alchemistPotionItemState.isEmpty() || state.oxidationLevel == 3) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().translateToHand(state, HumanoidArm.RIGHT, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(1.0F / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
        state.alchemistPotionItemState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
