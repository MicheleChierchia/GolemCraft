package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.client.model.BaseGolemModel;
import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class BaseGolemRenderer extends MobRenderer<BaseGolemEntity, BaseGolemRenderState, BaseGolemModel> {
    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/base_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_base_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_base_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_base_golem.png")
    };
    private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;

    public BaseGolemRenderer(EntityRendererProvider.Context context, net.minecraft.client.model.geom.ModelLayerLocation layer) {
        super(context, new BaseGolemModel(context.bakeLayer(layer)), 0.5F);
        this.itemModelResolver = context.getItemModelResolver();
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this));
        this.addLayer(new BaseGolemEyesLayer(this));
    }

    public BaseGolemRenderer(EntityRendererProvider.Context context) {
        this(context, com.golemcraft.golemcraftmod.events.ClientEvents.FLOWER_GOLEM_LAYER);
    }

    @Override
    public BaseGolemRenderState createRenderState() {
        return new BaseGolemRenderState();
    }

    @Override
    public void extractRenderState(BaseGolemEntity entity, BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        
        state.isRummaging = entity.isRummaging();
        state.oxidationLevel = entity.getOxidationLevel();
        state.mainArm = entity.getMainArm();

        // Read custom synced attack ticks for animation (10 ticks duration)
        int ticks = entity.getAttackAnimTicks();
        state.attackAnimProgress = ticks > 0 ? ticks / 10.0f : 0.0f;

        net.minecraft.world.item.ItemStack mainHandItem = entity.getMainHandItem();
        
        if (entity instanceof com.golemcraft.golemcraftmod.entity.FishermanGolemEntity fisherman && fisherman.isFishing() && mainHandItem.is(net.minecraft.world.item.Items.FISHING_ROD)) {
            mainHandItem = new net.minecraft.world.item.ItemStack(com.golemcraft.golemcraftmod.registry.ModBlocks.FISHING_ROD_CAST_DUMMY.get());
        }
        
        if (!mainHandItem.isEmpty()) {
            if (state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                this.itemModelResolver.updateForLiving(state.rightHandItemState, mainHandItem, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
                state.leftHandItemState.clear();
            } else {
                this.itemModelResolver.updateForLiving(state.leftHandItemState, mainHandItem, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
                state.rightHandItemState.clear();
            }
        } else {
            state.rightHandItemState.clear();
            state.leftHandItemState.clear();
        }
        
        state.isAggressive = entity.isAggressive();
        state.hasBow = !mainHandItem.isEmpty() && mainHandItem.is(net.minecraft.world.item.Items.BOW);
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
