package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class ExplorerGolemEyesLayer extends EyesLayer<BaseGolemRenderState, BaseGolemModel> {
    // When FOLLOWING (segue): Base Golem cyan/white eyes
    private static final RenderType FOLLOW_EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/base_golem_eyes.png"));
    // When STAYING (fermo): Explorer/Flower yellow eyes
    private static final RenderType STAY_EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/flower_golem_eyes.png"));

    private BaseGolemRenderState currentState;

    public ExplorerGolemEyesLayer(RenderLayerParent<BaseGolemRenderState, BaseGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        if (currentState != null && (currentState.isGuarding || currentState.isWaiting)) {
            return STAY_EYES;
        }
        return FOLLOW_EYES;
    }

    @Override
    public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int packedLight, BaseGolemRenderState state, float limbSwing, float limbSwingAmount) {
        this.currentState = state;
        if (state.oxidationLevel < 3) {
            super.submit(poseStack, submitNodeCollector, packedLight, state, limbSwing, limbSwingAmount);
        }
    }
}
