package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.entity.ai.DepositInChestGoal;
import com.golemcraft.golemcraftmod.entity.ai.LevelAreaGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;

public class LevelerGolemEntity extends BaseGolemEntity {

    public LevelerGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new LevelAreaGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new DepositInChestGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }

    public boolean hasShovel() {
        ItemStack mainHand = this.getItemInHand(InteractionHand.MAIN_HAND);
        return !mainHand.isEmpty() && (mainHand.getItem() instanceof ShovelItem || mainHand.is(net.minecraft.tags.ItemTags.SHOVELS));
    }

    public void syncShovelToHand() {
        if (!hasShovel()) {
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack stack = this.getInventory().getItem(i);
                if (!stack.isEmpty() && (stack.getItem() instanceof ShovelItem || stack.is(net.minecraft.tags.ItemTags.SHOVELS))) {
                    this.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                    this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                    this.getInventory().setItem(i, ItemStack.EMPTY);
                    return;
                }
            }
        }
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() && (itemstack.getItem() instanceof ShovelItem || itemstack.is(net.minecraft.tags.ItemTags.SHOVELS))) {
            if (!this.hasShovel()) {
                if (!this.level().isClientSide()) {
                    ItemStack shovelStack = itemstack.copy();
                    shovelStack.setCount(1);
                    this.setItemInHand(InteractionHand.MAIN_HAND, shovelStack);
                    this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                    this.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected boolean shouldSyncSlot0ToMainHand() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            syncShovelToHand();
        }
    }
}
