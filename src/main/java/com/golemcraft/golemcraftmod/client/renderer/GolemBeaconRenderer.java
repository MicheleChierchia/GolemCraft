package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.block.GolemBeaconBlock;
import com.golemcraft.golemcraftmod.block.GolemBeaconBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;

import javax.annotation.Nullable;

public class GolemBeaconRenderer implements BlockEntityRenderer<GolemBeaconBlockEntity, GolemBeaconRenderer.GolemBeaconRenderState> {

    private static final Identifier BEAM_TEXTURE =
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/misc/golem_beacon_beam.png");

    private static final float[][] BEAM_COLORS = {
            {0.91f, 0.48f, 0.29f}, // 0
            {0.77f, 0.66f, 0.51f}, // 1
            {0.43f, 0.68f, 0.53f}, // 2
            {0.30f, 0.72f, 0.55f}, // 3
    };

    public static class GolemBeaconRenderState extends BlockEntityRenderState {
        public boolean isPowered;
        public int oxidation;
        public float animationTime;
        public float beamRadiusScale;
    }

    public GolemBeaconRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public GolemBeaconRenderState createRenderState() {
        return new GolemBeaconRenderState();
    }

    @Override
    public void extractRenderState(GolemBeaconBlockEntity be, GolemBeaconRenderState state,
                                   float partialTicks, Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.isPowered = be.getBlockState().getValue(GolemBeaconBlock.POWERED);
        state.oxidation = be.getBlockState().getValue(GolemBeaconBlock.OXIDATION);

        state.animationTime = be.getLevel() != null ? Math.floorMod(be.getLevel().getGameTime(), 40) + partialTicks : 0.0F;

        float distanceToBeacon = (float)cameraPosition.subtract(Vec3.atCenterOf(state.blockPos)).horizontalDistance();
        var player = net.minecraft.client.Minecraft.getInstance().player;
        state.beamRadiusScale = player != null && player.isScoping() ? 1.0F : Math.max(1.0F, distanceToBeacon / 96.0F);
    }

    @Override
    public void submit(GolemBeaconRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.isPowered) return;

        float[] c = BEAM_COLORS[Math.max(0, Math.min(3, state.oxidation))];
        // Compattare RGB in ARGB (int) - Assumiamo alpha=255
        int a = 255;
        int r = (int)(c[0] * 255);
        int g = (int)(c[1] * 255);
        int b = (int)(c[2] * 255);
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        BeaconRenderer.submitBeaconBeam(
                poseStack,
                submitNodeCollector,
                BEAM_TEXTURE, // location
                state.beamRadiusScale, // scale
                state.animationTime,
                0, // beamStart
                1024, // height
                argb, // color
                0.15f * state.beamRadiusScale, // solid radius
                0.30f * state.beamRadiusScale // glow radius
        );
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(GolemBeaconBlockEntity be, Vec3 cameraPos) {
        return Vec3.atCenterOf(be.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPos.multiply(1.0, 0.0, 1.0), (double)this.getViewDistance());
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GolemBeaconBlockEntity blockEntity) {
        return net.minecraft.world.phys.AABB.INFINITE;
    }
}
