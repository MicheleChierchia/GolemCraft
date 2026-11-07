package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BeekeeperHoneyLayer extends RenderLayer<BaseGolemRenderState, BaseGolemModel> {
    private static final Identifier HONEY_TEXTURE = Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/beekeeper_honey_layer.png");
    private final BaseGolemModel model;

    public BeekeeperHoneyLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer, BaseGolemModel model) {
        super(renderer);
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        if (state.honeyLevel > 0) {
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            submitNodeCollector.order(1).submitModel(
                    this.model,
                    state,
                    poseStack,
                    RenderTypes.entityTranslucent(HONEY_TEXTURE),
                    packedLight,
                    overlayCoords,
                    state.outlineColor,
                    null
            );
        }
    }
}
