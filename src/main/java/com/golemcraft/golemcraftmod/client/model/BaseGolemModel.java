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
    protected final ModelPart root;
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;
    protected final ModelPart rightLeg;
    protected final ModelPart leftLeg;

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

    public static LayerDefinition createFishermanBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F)
                .texOffs(0, 40).addBox(-4.5F, -5.5F, -5.5F, 9.0F, 2.0F, 11.0F) // Fisherman hat brim
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
    public static LayerDefinition createLumberjackBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F)
                .texOffs(0, 40).addBox(-4.0F, -4.5F, -7.0F, 8.0F, 1.0F, 2.0F) // Front visor
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
        
        if (state.isFishing) {
            // (No sitting pose)

            // ── CORPO leggera inclinazione in avanti ──────────────────────
            this.body.xRot = 0.15F + Mth.sin(time * 0.05F) * 0.02F; // respiro lento

            // ── TESTA guarda verso l'acqua ────────────────────────────────
            // (yRot già impostato sopra, aggiungiamo solo un pitch verso il basso)
            this.head.xRot = 0.35F + Mth.sin(time * 0.07F) * 0.03F;

            // ── BRACCIA: impugnatura asimmetrica della canna ──────────────
            // Braccio destro: mano alta (impugnatura posteriore), leggermente
            // spostato verso sinistra per tenere la canna
            float rodSway = Mth.sin(time * 0.06F) * 0.04F; // micro-oscillazione d'attesa
            this.rightArm.xRot = -1.35F + rodSway;
            this.rightArm.zRot = -0.08F;
            this.rightArm.yRot =  0.12F;

            // Braccio sinistro: mano bassa (impugnatura anteriore)
            this.leftArm.xRot = -1.05F + rodSway;
            this.leftArm.zRot =  0.08F;
            this.leftArm.yRot = -0.12F;
        } else if (state.isRummaging) {
            this.body.xRot = 0.2F;
            this.head.xRot += 0.4F;
            this.rightArm.xRot = -1.2F;
            this.leftArm.xRot = -0.2F;
            this.rightArm.zRot = 0.0F;
        } else if (state.attackAnimProgress > 0.0F) {
            float t = 1.0f - state.attackAnimProgress;
            
            // Utilizza l'esatta formula matematica dell'animazione d'attacco di Minecraft (Player)
            float f = t; // progresso da 0.0 a 1.0
            this.body.yRot = Mth.sin(Mth.sqrt(f) * ((float)Math.PI * 2F)) * 0.2F;
            
            if (state.mainArm == net.minecraft.world.entity.HumanoidArm.LEFT) {
                this.body.yRot *= -1.0F;
            }
            
            ModelPart attackingArm = (state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT) ? this.rightArm : this.leftArm;
            
            float f1 = Mth.sin(f * (float)Math.PI);
            float f2 = Mth.sin((1.0F - (1.0F - f) * (1.0F - f)) * (float)Math.PI);
            
            attackingArm.xRot -= f2 * 1.2F + f1 * 0.4F;
            attackingArm.yRot += this.body.yRot * 2.0F;
            attackingArm.zRot += Mth.sin(f * (float)Math.PI) * (state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? -0.4F : 0.4F);
        } else {
            this.body.xRot = 0.0F;
            this.body.yRot = 0.0F;
            this.rightArm.xRot += Mth.sin(time * 0.06F) * 0.05F;
            this.leftArm.xRot  -= Mth.sin(time * 0.06F) * 0.05F;
        }

        if (state.isAggressive && state.hasBow) {
            if (state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                this.rightArm.yRot = -0.1F + this.head.yRot;
                this.leftArm.yRot = 0.1F + this.head.yRot + 0.4F;
                this.rightArm.xRot = -((float)Math.PI / 2F) + this.head.xRot;
                this.leftArm.xRot = -((float)Math.PI / 2F) + this.head.xRot;
            } else {
                this.leftArm.yRot = 0.1F + this.head.yRot;
                this.rightArm.yRot = -0.1F + this.head.yRot - 0.4F;
                this.rightArm.xRot = -((float)Math.PI / 2F) + this.head.xRot;
                this.leftArm.xRot = -((float)Math.PI / 2F) + this.head.xRot;
            }
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
