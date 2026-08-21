package com.golemcraft.golemcraftmod.client.model;

import com.golemcraft.golemcraftmod.client.renderer.BaseGolemRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.client.model.ArmedModel;

public class BaseGolemModel extends EntityModel<BaseGolemRenderState> implements ArmedModel {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public BaseGolemModel(ModelPart root) {
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

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F)
                .texOffs(37, 8).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F) // Parafulmine (Base sottile)
                .texOffs(37, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F) // Parafulmine (Punta grossa)
                .texOffs(56, 0).addBox(-1.0F, -3.0F, -7.0F, 2.0F, 3.0F, 2.0F), // Naso
                PartPose.offset(0.0F, 13.0F, 0.0F));

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

    public static LayerDefinition createFarmerBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F)
                .texOffs(0, 40).addBox(-5.0F, -4.5F, -6.0F, 10.0F, 1.0F, 12.0F) // Tesa del cappello di paglia
                .texOffs(37, 8).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F) // Parafulmine (Base sottile)
                .texOffs(37, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F) // Parafulmine (Punta grossa)
                .texOffs(56, 0).addBox(-1.0F, -3.0F, -7.0F, 2.0F, 3.0F, 2.0F), // Naso
                PartPose.offset(0.0F, 13.0F, 0.0F));

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
        
        if (state.oxidationLevel == 3) {
            this.head.yRot = state.yRot * ((float)Math.PI / 180F);
            this.head.xRot = state.xRot * ((float)Math.PI / 180F);
            this.head.zRot = 0.0F;
            this.rightLeg.xRot = -1.4F;
            this.leftLeg.xRot = -1.4F;
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.body.xRot = 0.0F;
            
            this.head.y = 13.0F + 5.0F;
            this.body.y = 13.0F + 5.0F;
            this.rightArm.y = 13.0F + 5.0F;
            this.leftArm.y = 13.0F + 5.0F;
            this.rightLeg.y = 22.0F;
            this.leftLeg.y = 22.0F;
            this.rightLeg.z = -3.0F;
            this.leftLeg.z = -3.0F;
            return;
        }
        
        // Reset Y and Z
        this.head.y = 13.0F;
        this.body.y = 13.0F;
        this.rightArm.y = 13.0F;
        this.leftArm.y = 13.0F;
        this.rightLeg.y = 19.0F;
        this.leftLeg.y = 19.0F;
        this.rightLeg.z = 0.0F;
        this.leftLeg.z = 0.0F;
        
        this.head.yRot = state.yRot * ((float)Math.PI / 180F);
        this.head.xRot = state.xRot * ((float)Math.PI / 180F);
        this.head.zRot = 0.0F; // Reset zRot to avoid glitches
        
        float time = state.ageInTicks;
        // Animazione testa stile Copper Golem: la testa fa un giro completo di 360 gradi ogni tanto
        if (time % 160 < 20) {
            float spinProgress = (time % 160) / 20.0F; // 0.0 to 1.0
            this.head.yRot += spinProgress * ((float)Math.PI * 2F);
        } else {
            this.head.zRot = Mth.sin(time * 0.1F) * 0.05F;
        }

        // Camminata esagerata e goffa
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 1.4F * state.walkAnimationSpeed;
        
        // Braccia che si muovono molto (stile burattino)
        this.rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 2.0F * state.walkAnimationSpeed;
        this.leftArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 2.0F * state.walkAnimationSpeed;
        
        // Movimento idle per le braccia
        this.rightArm.zRot = (Mth.cos(time * 0.09F) * 0.05F + 0.05F);
        this.leftArm.zRot = -(Mth.cos(time * 0.09F) * 0.05F + 0.05F);
        
        if (state.isRummaging) {
            this.body.xRot = 0.2F; // Si piega leggermente in avanti
            this.head.xRot += 0.4F; // Guarda giù verso la cassa
            
            // Allunga il braccio destro nella cassa come fa il Copper Golem col bottone
            this.rightArm.xRot = -1.2F;
            this.leftArm.xRot = -0.2F; // Il braccio sinistro resta più rilassato
            this.rightArm.zRot = 0.0F;
        } else {
            this.body.xRot = 0.0F;
            this.rightArm.xRot += Mth.sin(time * 0.06F) * 0.05F;
            this.leftArm.xRot -= Mth.sin(time * 0.06F) * 0.05F;
        }
    }

    @Override
    public void translateToHand(net.minecraft.client.renderer.entity.state.EntityRenderState state, net.minecraft.world.entity.HumanoidArm arm, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        if (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
            this.rightArm.translateAndRotate(poseStack);
        } else {
            this.leftArm.translateAndRotate(poseStack);
        }
    }
}
