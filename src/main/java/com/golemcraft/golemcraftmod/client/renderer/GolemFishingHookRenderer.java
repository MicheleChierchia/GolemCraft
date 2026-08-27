package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.entity.FishermanGolemEntity;
import com.golemcraft.golemcraftmod.entity.projectile.GolemFishingHook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GolemFishingHookRenderer extends EntityRenderer<GolemFishingHook, GolemFishingHookRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.fromNamespaceAndPath(com.golemcraft.golemcraftmod.GolemCraft.MODID, "textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE_LOCATION);

    public GolemFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GolemFishingHook p_363069_, Frustum p_362635_, double p_361840_, double p_361502_, double p_360380_) {
        return super.shouldRender(p_363069_, p_362635_, p_361840_, p_361502_, p_360380_) && p_363069_.getGolemOwner() != null;
    }

    @Override
    public GolemFishingHookRenderState createRenderState() {
        return new GolemFishingHookRenderState();
    }

    @Override
    public void extractRenderState(GolemFishingHook hook, GolemFishingHookRenderState state, float partialTicks) {
        super.extractRenderState(hook, state, partialTicks);
        FishermanGolemEntity golem = hook.getGolemOwner();
        if (golem != null) {
            Vec3 hookPos = hook.getPosition(partialTicks).add(0.0, 0.25, 0.0);
            
            double golemYRot = Mth.lerp(partialTicks, golem.yRotO, golem.getYRot());
            double yawRad = Math.toRadians(golemYRot);
            double fwdX = -Math.sin(yawRad);
            double fwdZ = Math.cos(yawRad);
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);
            
            double golemX = Mth.lerp(partialTicks, golem.xo, golem.getX());
            double golemY = Mth.lerp(partialTicks, golem.yo, golem.getY());
            double golemZ = Mth.lerp(partialTicks, golem.zo, golem.getZ());
            
            double handX = golemX + fwdX * 0.4 - rightX * 0.7;
            double handY = golemY + 1.35; 
            double handZ = golemZ + fwdZ * 0.4 - rightZ * 0.7;
            
            Vec3 handPos = new Vec3(handX, handY, handZ);
            state.lineOriginOffset = handPos.subtract(hookPos);
        } else {
            state.lineOriginOffset = Vec3.ZERO;
        }
    }

    @Override
    public void submit(GolemFishingHookRenderState p_451173_, PoseStack p_434862_, SubmitNodeCollector p_433298_, CameraRenderState p_451318_) {
        p_434862_.pushPose();
        p_434862_.pushPose();
        p_434862_.scale(0.5F, 0.5F, 0.5F);
        p_434862_.mulPose(p_451318_.orientation);
        p_433298_.submitCustomGeometry(p_434862_, RENDER_TYPE, (p_434943_, p_432878_) -> {
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 0.0F, 0, 0, 1);
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 1.0F, 0, 1, 1);
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 1.0F, 1, 1, 0);
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 0.0F, 1, 0, 0);
        });
        p_434862_.popPose();
        
        float f = (float)p_451173_.lineOriginOffset.x;
        float f1 = (float)p_451173_.lineOriginOffset.y;
        float f2 = (float)p_451173_.lineOriginOffset.z;
        float f3 = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        
        p_433298_.submitCustomGeometry(p_434862_, RenderTypes.lines(), (p_454362_, p_454363_) -> {
            for (int j = 0; j < 16; j++) {
                float f4 = fraction(j, 16);
                float f5 = fraction(j + 1, 16);
                stringVertex(f, f1, f2, p_454363_, p_454362_, f4, f5, f3);
                stringVertex(f, f1, f2, p_454363_, p_454362_, f5, f4, f3);
            }
        });
        
        p_434862_.popPose();
        super.submit(p_451173_, p_434862_, p_433298_, p_451318_);
    }

    private static float fraction(int p_114691_, int p_114692_) {
        return (float)p_114691_ / (float)p_114692_;
    }

    private static void vertex(VertexConsumer p_254464_, PoseStack.Pose p_323724_, int p_254296_, float p_253632_, int p_254132_, int p_254171_, int p_254026_) {
        p_254464_.addVertex(p_323724_, p_253632_ - 0.5F, (float)p_254132_ - 0.5F, 0.0F)
            .setColor(-1)
            .setUv((float)p_254171_, (float)p_254026_)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(p_254296_)
            .setNormal(p_323724_, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(
        float p_174119_,
        float p_174120_,
        float p_174121_,
        VertexConsumer p_174122_,
        PoseStack.Pose p_174123_,
        float p_174124_,
        float p_174125_,
        float p_455125_
    ) {
        float f = p_174119_ * p_174124_;
        float f1 = p_174120_ * (p_174124_ * p_174124_ + p_174124_) * 0.5F + 0.25F;
        float f2 = p_174121_ * p_174124_;
        float f3 = p_174119_ * p_174125_ - f;
        float f4 = p_174120_ * (p_174125_ * p_174125_ + p_174125_) * 0.5F + 0.25F - f1;
        float f5 = p_174121_ * p_174125_ - f2;
        float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
        f3 /= f6;
        f4 /= f6;
        f5 /= f6;
        p_174122_.addVertex(p_174123_, f, f1, f2).setColor(-16777216).setNormal(p_174123_, f3, f4, f5).setLineWidth(p_455125_);
    }
}
