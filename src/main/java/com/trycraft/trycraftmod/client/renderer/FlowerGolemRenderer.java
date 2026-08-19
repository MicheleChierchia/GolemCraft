package com.trycraft.trycraftmod.client.renderer;

import com.trycraft.trycraftmod.TryCraft;
import com.trycraft.trycraftmod.client.model.FlowerGolemModel;
import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FlowerGolemRenderer extends MobRenderer<FlowerGolemEntity, FlowerGolemRenderState, FlowerGolemModel> {
    private static final Identifier GOLEM_LOCATION = Identifier.fromNamespaceAndPath(TryCraft.MODID, "textures/entity/flower_golem.png");
    private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;

    public FlowerGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new FlowerGolemModel(context.bakeLayer(com.trycraft.trycraftmod.events.ClientEvents.FLOWER_GOLEM_LAYER)), 0.5F);
        this.itemModelResolver = context.getItemModelResolver();
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this));
    }

    @Override
    public FlowerGolemRenderState createRenderState() {
        return new FlowerGolemRenderState();
    }

    @Override
    public void extractRenderState(FlowerGolemEntity entity, FlowerGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        
        state.isRummaging = entity.isRummaging();
        state.mainArm = entity.getMainArm();
        net.minecraft.world.item.ItemStack mainHandItem = entity.getMainHandItem();
        
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
    }

    @Override
    public Identifier getTextureLocation(FlowerGolemRenderState state) {
        return GOLEM_LOCATION;
    }
}
