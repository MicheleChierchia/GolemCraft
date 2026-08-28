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
        if (state.oxidationLevel == 3) {
            super.setupAnim(state);
            return;
        }
        
        // Reset base positions and rotations cleanly (no puppet artifacts)
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
        this.head.zRot = 0.0F;
        
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        
        float time = state.ageInTicks;
        
        // Heavy, lumbering walk animation for legs
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 1.2F * state.walkAnimationSpeed;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        // Tendrils organic sway
        float tendrilTime = time * 0.15F;
        float wiggle = Mth.sin(tendrilTime) * 0.12F;
        float wiggle2 = Mth.cos(tendrilTime * 0.8F) * 0.12F;
        this.leftTendril.xRot = wiggle;
        this.leftTendril.zRot = 0.25F + wiggle2;
        this.rightTendril.xRot = -wiggle;
        this.rightTendril.zRot = -0.25F - wiggle2;

        if (state.attackAnimProgress > 0.0F) {
            float f = 1.0F - Mth.clamp(state.attackAnimProgress, 0.0F, 1.0F); // 0.0 (start of attack) -> 1.0 (end of attack)
            
            // Continuous harmonic curves
            float swing = Mth.sin(f * (float)Math.PI); // 0.0 -> 1.0 -> 0.0 (arms spread wide)
            float strike = Mth.sin((1.0F - (1.0F - f) * (1.0F - f)) * (float)Math.PI); // forward smash curve
            
            // Torso & head lean back on windup, then slam forward on smash
            float bodySlam = -Mth.sin(f * (float)Math.PI * 2.0F);
            this.body.xRot = bodySlam * 0.35F;
            this.body.yRot = 0.0F;
            this.head.xRot = (state.xRot * ((float)Math.PI / 180F)) + bodySlam * 0.25F;
            
            // Both arms raise high and spread far wide open (Warden Roar pose), then smash forward together
            this.rightArm.xRot = -strike * 1.9F;
            this.rightArm.zRot = swing * 1.4F;
            this.rightArm.yRot = -swing * 0.35F;
            
            this.leftArm.xRot = -strike * 1.9F;
            this.leftArm.zRot = -swing * 1.4F;
            this.leftArm.yRot = swing * 0.35F;
            
            // Tendrils flutter with intensity
            this.leftTendril.zRot += swing * 0.4F;
            this.rightTendril.zRot -= swing * 0.4F;
        } else if (state.isAggressive) {
            // Menacing stalking stance
            this.body.xRot = 0.18F;
            this.head.xRot += 0.05F;
            
            // Arms spread slightly outward and ready to strike
            float armSway = Mth.cos(time * 0.1F) * 0.04F;
            this.rightArm.xRot = 0.2F + Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 0.8F * state.walkAnimationSpeed;
            this.leftArm.xRot = 0.2F + Mth.cos(state.walkAnimationPos * 0.6662F) * 0.8F * state.walkAnimationSpeed;
            this.rightArm.zRot = 0.45F + armSway;
            this.leftArm.zRot = -0.45F - armSway;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            
            // Tendrils twitch excitedly when sensing vibrations
            float flutter = Mth.sin(time * 0.6F) * 0.18F;
            this.leftTendril.zRot += flutter;
            this.rightTendril.zRot -= flutter;
        } else {
            // Calm idle & walk arm sway
            this.rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float)Math.PI) * 1.4F * state.walkAnimationSpeed;
            this.leftArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
            this.rightArm.zRot = 0.15F + (Mth.cos(time * 0.08F) * 0.05F);
            this.leftArm.zRot = -0.15F - (Mth.cos(time * 0.08F) * 0.05F);
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
        }
    }
}
