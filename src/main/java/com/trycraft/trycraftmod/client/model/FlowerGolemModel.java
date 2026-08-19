package com.trycraft.trycraftmod.client.model;

import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
import com.trycraft.trycraftmod.client.renderer.FlowerGolemRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class FlowerGolemModel extends EntityModel<FlowerGolemRenderState> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public FlowerGolemModel(ModelPart root) {
        super(root);
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -5.0F, -2.0F, 6.0F, 5.0F, 4.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(-3.0F, 8.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(0.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(3.0F, 8.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offset(-1.9F, 13.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offset(1.9F, 13.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }


    @Override
    public void setupAnim(FlowerGolemRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180F);
        this.head.xRot = state.xRot * ((float)Math.PI / 180F);
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 1.4F * state.walkAnimationSpeed;
        this.rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 1.4F * state.walkAnimationSpeed;
        this.leftArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
    }
}
