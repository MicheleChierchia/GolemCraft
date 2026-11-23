package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import com.golemcraft.golemcraftmod.entity.MarineGolemEntity;
import com.golemcraft.golemcraftmod.events.ClientEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MarineGolemRenderer extends BaseGolemRenderer {

    private static final Identifier[] MARINE_LOCATIONS = new Identifier[]{
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/marine_golem.png"),
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_marine_golem.png"),
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_marine_golem.png"),
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_marine_golem.png")
    };
    private static final Identifier MARINE_EYES_TEXTURE =
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/marine_golem_eyes.png");
    private static final Identifier GUARDIAN_BEAM_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/guardian/guardian_beam.png");
    private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutout(GUARDIAN_BEAM_LOCATION);

    public MarineGolemRenderer(EntityRendererProvider.Context context) {
        super(context, ClientEvents.MARINE_GOLEM_LAYER);
        this.addLayer(new GolemEyesLayer(this, MARINE_EYES_TEXTURE));
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return MARINE_LOCATIONS[state.oxidationLevel];
    }

    @Override
    public boolean shouldRender(BaseGolemEntity entity, Frustum culler, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, culler, camX, camY, camZ)) {
            return true;
        }
        if (entity instanceof MarineGolemEntity marine && marine.hasActiveLaserTarget()) {
            LivingEntity target = marine.getActiveLaserTarget();
            if (target != null) {
                Vec3 targetPos = getEntityPos(target, target.getBbHeight() * 0.5D, 1.0F);
                Vec3 startPos = getEntityPos(marine, MarineGolemEntity.SEA_LANTERN_Y_OFFSET, 1.0F);
                return culler.isVisible(new AABB(startPos.x, startPos.y, startPos.z, targetPos.x, targetPos.y, targetPos.z));
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(BaseGolemEntity entity, BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof MarineGolemEntity marine) {
            state.isGuarding = marine.isStaying();
            state.isSwimming = marine.isInWater() && marine.getOxidationLevel() < 3;
            state.swimAmount = marine.getSwimAmount(partialTick);
            LivingEntity target = marine.getActiveLaserTarget();
            if (target != null) {
                state.laserOriginPosition = getEntityPos(marine, MarineGolemEntity.SEA_LANTERN_Y_OFFSET, partialTick);
                state.laserTargetPosition = getEntityPos(target, target.getBbHeight() * 0.5D, partialTick);
                state.laserAttackScale = marine.getLaserAttackScale(partialTick);
                state.laserAttackTime = marine.getClientSideAttackTime() + partialTick;
                return;
            }
        }
        state.laserOriginPosition = null;
        state.laserTargetPosition = null;
    }

    @Override
    protected void setupRotations(BaseGolemRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (state.swimAmount > 0.0F) {
            float pitch = -90.0F - state.xRot;
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(state.swimAmount, 0.0F, pitch)));
            if (state.isSwimming) {
                poseStack.translate(0.0F, -0.5F, 0.25F);
            }
        }
    }

    @Override
    public void submit(BaseGolemRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        if (state.laserTargetPosition != null && state.laserOriginPosition != null) {
            float texVOff = state.laserAttackTime * 0.5F % 1.0F;
            poseStack.pushPose();
            poseStack.translate(0.0F, (float) MarineGolemEntity.SEA_LANTERN_Y_OFFSET, 0.0F);
            renderBeam(
                    poseStack,
                    submitNodeCollector,
                    state.laserTargetPosition.subtract(state.laserOriginPosition),
                    state.laserAttackTime,
                    state.laserAttackScale,
                    texVOff
            );
            poseStack.popPose();
        }
    }

    private static Vec3 getEntityPos(LivingEntity entity, double yOffset, float partialTicks) {
        double sx = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double sy = Mth.lerp(partialTicks, entity.yOld, entity.getY()) + yOffset;
        double sz = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        return new Vec3(sx, sy, sz);
    }

    private static void renderBeam(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Vec3 beamVector,
                                   float timeInTicks, float scale, float texVOff) {
        float length = (float) (beamVector.length() + 1.0D);
        beamVector = beamVector.normalize();
        float xRot = (float) Math.acos(beamVector.y);
        float yRot = (float) (Math.PI / 2.0D) - (float) Math.atan2(beamVector.z, beamVector.x);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot * (180.0F / (float) Math.PI)));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot * (180.0F / (float) Math.PI)));
        float rot = timeInTicks * 0.05F * -1.5F;
        float colorScale = scale * scale;
        int red = 64 + (int) (colorScale * 191.0F);
        int green = 32 + (int) (colorScale * 191.0F);
        int blue = 128 - (int) (colorScale * 64.0F);
        float wnx = Mth.cos(rot + (float) (Math.PI * 3.0D / 4.0D)) * 0.282F;
        float wnz = Mth.sin(rot + (float) (Math.PI * 3.0D / 4.0D)) * 0.282F;
        float enx = Mth.cos(rot + (float) (Math.PI / 4.0D)) * 0.282F;
        float enz = Mth.sin(rot + (float) (Math.PI / 4.0D)) * 0.282F;
        float wsx = Mth.cos(rot + (float) (Math.PI * 5.0D / 4.0D)) * 0.282F;
        float wsz = Mth.sin(rot + (float) (Math.PI * 5.0D / 4.0D)) * 0.282F;
        float esx = Mth.cos(rot + (float) (Math.PI * 7.0D / 4.0D)) * 0.282F;
        float esz = Mth.sin(rot + (float) (Math.PI * 7.0D / 4.0D)) * 0.282F;
        float wx = Mth.cos(rot + (float) Math.PI) * 0.2F;
        float wz = Mth.sin(rot + (float) Math.PI) * 0.2F;
        float ex = Mth.cos(rot) * 0.2F;
        float ez = Mth.sin(rot) * 0.2F;
        float nx = Mth.cos(rot + (float) (Math.PI / 2.0D)) * 0.2F;
        float nz = Mth.sin(rot + (float) (Math.PI / 2.0D)) * 0.2F;
        float sx = Mth.cos(rot + (float) (Math.PI * 3.0D / 2.0D)) * 0.2F;
        float sz = Mth.sin(rot + (float) (Math.PI * 3.0D / 2.0D)) * 0.2F;
        float minV = -1.0F + texVOff;
        float maxV = minV + length * 2.5F;
        submitNodeCollector.submitCustomGeometry(poseStack, BEAM_RENDER_TYPE, (pose, buffer) -> {
            beamVertex(buffer, pose, wx, length, wz, red, green, blue, 0.4999F, maxV);
            beamVertex(buffer, pose, wx, 0.0F, wz, red, green, blue, 0.4999F, minV);
            beamVertex(buffer, pose, ex, 0.0F, ez, red, green, blue, 0.0F, minV);
            beamVertex(buffer, pose, ex, length, ez, red, green, blue, 0.0F, maxV);
            beamVertex(buffer, pose, nx, length, nz, red, green, blue, 0.4999F, maxV);
            beamVertex(buffer, pose, nx, 0.0F, nz, red, green, blue, 0.4999F, minV);
            beamVertex(buffer, pose, sx, 0.0F, sz, red, green, blue, 0.0F, minV);
            beamVertex(buffer, pose, sx, length, sz, red, green, blue, 0.0F, maxV);
            float vBase = Mth.floor(timeInTicks) % 2 == 0 ? 0.5F : 0.0F;
            beamVertex(buffer, pose, wnx, length, wnz, red, green, blue, 0.5F, vBase + 0.5F);
            beamVertex(buffer, pose, enx, length, enz, red, green, blue, 1.0F, vBase + 0.5F);
            beamVertex(buffer, pose, esx, length, esz, red, green, blue, 1.0F, vBase);
            beamVertex(buffer, pose, wsx, length, wsz, red, green, blue, 0.5F, vBase);
        });
    }

    private static void beamVertex(VertexConsumer builder, PoseStack.Pose pose, float x, float y, float z,
                                   int red, int green, int blue, float u, float v) {
        builder.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
