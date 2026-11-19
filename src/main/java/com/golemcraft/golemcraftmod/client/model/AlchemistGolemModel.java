package com.golemcraft.golemcraftmod.client.model;

import com.golemcraft.golemcraftmod.client.renderer.BaseGolemRenderState;
import com.golemcraft.golemcraftmod.entity.AlchemistGolemEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class AlchemistGolemModel extends BaseGolemModel {

    private final ModelPart nose;

    public AlchemistGolemModel(ModelPart root) {
        super(root);
        this.nose = this.head.getChild("nose");
    }

    public static LayerDefinition createAlchemistBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 36).addBox(0.0F, 0.0F, 0.0F, 10.0F, 2.0F, 12.0F),
                PartPose.offset(-5.0F, -6.5F, -6.0F));

        PartDefinition hat2 = hat.addOrReplaceChild("hat2", CubeListBuilder.create()
                .texOffs(0, 50).addBox(0.0F, 0.0F, 0.0F, 7.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(1.5F, -3.5F, 2.5F, -0.05235988F, 0.0F, 0.02617994F));

        PartDefinition hat3 = hat2.addOrReplaceChild("hat3", CubeListBuilder.create()
                .texOffs(28, 50).addBox(0.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(1.5F, -3.5F, 1.5F, -0.10471976F, 0.0F, 0.05235988F));

        hat3.addOrReplaceChild("hat4", CubeListBuilder.create()
                .texOffs(44, 50).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(1.5F, -2.0F, 1.5F, (float) (-Math.PI / 15.0), 0.0F, 0.10471976F));

        PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -3.0F, -5.0F));

        nose.addOrReplaceChild("mole", CubeListBuilder.create()
                .texOffs(48, 50).addBox(0.25F, 1.25F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)),
                PartPose.ZERO);

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

    public static LayerDefinition createAlchemistRobeLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 13.0F, 0.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, -5.0F));

        partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 8.0F, 6.0F, new CubeDeformation(0.5F))
                .texOffs(0, 16).addBox(-4.5F, -0.5F, -3.5F, 9.0F, 2.0F, 7.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 7.0F, 4.0F, new CubeDeformation(0.35F)),
                PartPose.offset(-5.5F, 13.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 7.0F, 4.0F, new CubeDeformation(0.35F)),
                PartPose.offset(5.5F, 13.0F, 0.0F));

        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.0F, 19.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0F, 19.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(BaseGolemRenderState state) {
        super.setupAnim(state);

        this.nose.x = 0.0F;
        this.nose.y = -3.0F;
        this.nose.z = -5.0F;
        this.nose.xRot = 0.0F;
        this.nose.yRot = 0.0F;
        this.nose.zRot = 0.0F;

        if (state.oxidationLevel == 3) {
            return;
        }

        if (!state.isWaiting && state.alchemistPotionAction != AlchemistGolemEntity.POTION_DRINKING_INVISIBILITY) {
            this.nose.xRot = Mth.sin(state.ageInTicks * 0.02F) * 4.5F * ((float) Math.PI / 180.0F);
            this.nose.zRot = Mth.cos(state.ageInTicks * 0.02F) * 2.5F * ((float) Math.PI / 180.0F);
        }

        if (state.alchemistPotionAction == AlchemistGolemEntity.POTION_DRINKING_INVISIBILITY) {
            this.head.yRot = state.yRot * ((float) Math.PI / 180.0F);
            this.head.xRot = state.xRot * ((float) Math.PI / 180.0F);
            this.head.zRot = 0.0F;

            float sipBob = Mth.sin(state.ageInTicks * 0.6F) * 0.08F;
            this.rightArm.xRot = -1.20F + sipBob;
            this.rightArm.yRot = -0.48F;
            this.rightArm.zRot = 0.0F;
        } else if (!state.isWaiting && state.alchemistPotionAction == AlchemistGolemEntity.POTION_THROWING_HEALING) {
            this.rightArm.xRot = -2.1F;
            this.rightArm.yRot = -0.15F;
            this.rightArm.zRot = 0.1F;
        }
    }
}
