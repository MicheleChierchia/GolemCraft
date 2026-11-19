package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.client.model.AlchemistGolemModel;
import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

public class AlchemistRobeLayer extends RenderLayer<BaseGolemRenderState, BaseGolemModel> {

    private static final Identifier ROBE_TEXTURE =
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/alchemist_golem_robe.png");

    private final AlchemistGolemModel robeModel;

    public AlchemistRobeLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer, AlchemistGolemModel robeModel) {
        super(renderer);
        this.robeModel = robeModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        coloredCutoutModelCopyLayerRender(this.robeModel, ROBE_TEXTURE, poseStack, submitNodeCollector, packedLight, state, -1, 1);
    }
}
