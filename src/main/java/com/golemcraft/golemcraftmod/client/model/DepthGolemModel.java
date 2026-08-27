package com.golemcraft.golemcraftmod.client.model;

import com.golemcraft.golemcraftmod.client.renderer.BaseGolemRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class DepthGolemModel extends BaseGolemModel {
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;

    public DepthGolemModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild("head");
        this.leftTendril = head.getChild("left_tendril");
        this.rightTendril = head.getChild("right_tendril");
    }

    public static LayerDefinition createDepthBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F)
                .texOffs(37, 8).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F) // Parafulmine (Base sottile)
                .texOffs(37, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F) // Parafulmine (Punta grossa)
                .texOffs(56, 0).addBox(-1.0F, -3.0F, -7.0F, 2.0F, 3.0F, 2.0F), // Naso
                PartPose.offset(0.0F, 13.0F, 0.0F));

        head.addOrReplaceChild("left_tendril", CubeListBuilder.create()
                .texOffs(56, 32).addBox(-1.5F, -5.0F, 0.0F, 3.0F, 5.0F, 0.0F),
                PartPose.offset(2.0F, -9.0F, 0.0F));

        head.addOrReplaceChild("right_tendril", CubeListBuilder.create()
                .texOffs(56, 32).addBox(-1.5F, -5.0F, 0.0F, 3.0F, 5.0F, 0.0F),
                PartPose.offset(-2.0F, -9.0F, 0.0F));

        partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 15).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(36, 16).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offset(-5.5F, 13.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(50, 16).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offset(5.5F, 13.0F, 0.0F));

        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(-2.0F, 19.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(16, 27).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(2.0F, 19.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(BaseGolemRenderState state) {
        super.setupAnim(state);
        // Tendrils animation like sculk sensor leaves
        float time = state.ageInTicks * 0.15F;
        float wiggle = Mth.sin(time) * 0.15F;
        float wiggle2 = Mth.cos(time * 0.8F) * 0.15F;
        
        // Bend them outwards by default (zRot) and wave them
        this.leftTendril.xRot = wiggle;
        this.leftTendril.zRot = 0.3F + wiggle2;
        
        this.rightTendril.xRot = -wiggle;
        this.rightTendril.zRot = -0.3F - wiggle2;
    }
}
