package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class TotemGolemEyesLayer extends EyesLayer<BaseGolemRenderState, BaseGolemModel> {
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/totem_golem_eyes.png"));

    public TotemGolemEyesLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }

    @Override
    public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        super.submit(poseStack, submitNodeCollector, packedLight, state, limbSwing, limbSwingAmount);
    }
}
