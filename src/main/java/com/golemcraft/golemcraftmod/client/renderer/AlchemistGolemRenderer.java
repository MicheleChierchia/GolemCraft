package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.client.model.AlchemistGolemModel;
import com.golemcraft.golemcraftmod.entity.AlchemistGolemEntity;
import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import com.golemcraft.golemcraftmod.events.ClientEvents;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class AlchemistGolemRenderer extends BaseGolemRenderer {

    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/alchemist_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_alchemist_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_alchemist_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_alchemist_golem.png")
    };

    private ItemStack invisibilityPotionStack;
    private ItemStack healingSplashStack;

    public AlchemistGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new AlchemistGolemModel(context.bakeLayer(ClientEvents.ALCHEMIST_GOLEM_LAYER)));
        this.addLayer(new GolemEyesLayer(
                this,
                Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/alchemist_golem_eyes.png"),
                state -> state.oxidationLevel < 3 && !state.isInvisible
        ));
        this.addLayer(new AlchemistRobeLayer(this, new AlchemistGolemModel(context.bakeLayer(ClientEvents.ALCHEMIST_GOLEM_ROBE_LAYER))));
        this.addLayer(new AlchemistPotionLayer(this));
    }

    private ItemStack getInvisibilityPotionStack() {
        if (this.invisibilityPotionStack == null) {
            this.invisibilityPotionStack = PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY);
        }
        return this.invisibilityPotionStack;
    }

    private ItemStack getHealingSplashStack() {
        if (this.healingSplashStack == null) {
            this.healingSplashStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        }
        return this.healingSplashStack;
    }

    @Override
    public void extractRenderState(BaseGolemEntity entity, BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof AlchemistGolemEntity alchemist) {
            state.isWaiting = alchemist.isStaying();
            state.isGuarding = alchemist.isStaying();
            state.alchemistPotionAction = alchemist.getPotionAction();

            if (state.alchemistPotionAction == AlchemistGolemEntity.POTION_DRINKING_INVISIBILITY) {
                this.itemModelResolver.updateForLiving(
                        state.alchemistPotionItemState,
                        getInvisibilityPotionStack(),
                        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        entity
                );
            } else if (state.alchemistPotionAction == AlchemistGolemEntity.POTION_THROWING_HEALING) {
                this.itemModelResolver.updateForLiving(
                        state.alchemistPotionItemState,
                        getHealingSplashStack(),
                        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        entity
                );
            } else {
                state.alchemistPotionItemState.clear();
            }
        } else {
            state.alchemistPotionAction = AlchemistGolemEntity.POTION_NONE;
            state.alchemistPotionItemState.clear();
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
