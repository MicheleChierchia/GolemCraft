package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.FlowerGolemEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

public class PickupFlowerGoal extends Goal {
    private final FlowerGolemEntity golem;
    private ItemEntity targetItem;

    public PickupFlowerGoal(FlowerGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.golem.getMainHandItem().isEmpty()) {
            return false; // Hands full
        }

        List<ItemEntity> items = this.golem.level().getEntitiesOfClass(ItemEntity.class, this.golem.getBoundingBox().inflate(16.0D, 8.0D, 16.0D));
        for (ItemEntity item : items) {
            if (isFlower(item.getItem())) {
                this.targetItem = item;
                return true;
            }
        }
        return false;
    }

    private boolean isFlower(ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.SMALL_FLOWERS) || stack.is(net.minecraft.tags.ItemTags.FLOWERS)) return true;
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            return blockItem.getBlock() instanceof net.minecraft.world.level.block.FlowerBlock || blockItem.getBlock() instanceof net.minecraft.world.level.block.TallFlowerBlock;
        }
        return false;
    }

    @Override
    public void start() {
        this.golem.getNavigation().moveTo(this.targetItem, 1.2D);
    }

    @Override
    public void tick() {
        if (this.targetItem != null && this.targetItem.isAlive()) {
            this.golem.getNavigation().moveTo(this.targetItem, 1.2D);
            if (this.golem.getBoundingBox().inflate(1.0D).intersects(this.targetItem.getBoundingBox())) {
                // Pick up the item
                ItemStack stack = this.targetItem.getItem().copy();
                this.golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, stack);
                this.targetItem.discard();
                this.golem.level().playSound(null, this.golem.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                this.targetItem = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetItem != null && this.targetItem.isAlive() && this.golem.getMainHandItem().isEmpty();
    }

    @Override
    public void stop() {
        this.targetItem = null;
        this.golem.getNavigation().stop();
    }
}
