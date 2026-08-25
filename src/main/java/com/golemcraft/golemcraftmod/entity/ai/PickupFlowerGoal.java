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
        // Need space
        // Check dynamically inside loop


        List<ItemEntity> items = this.golem.level().getEntitiesOfClass(ItemEntity.class, this.golem.getBoundingBox().inflate(16.0D, 8.0D, 16.0D));
        for (ItemEntity item : items) {
            if (isFlower(item.getItem()) && hasSpaceFor(item.getItem())) {
                this.targetItem = item;
                return true;
            }
        }
        return false;
    }

    private boolean hasSpaceFor(ItemStack stack) {
        net.minecraft.world.SimpleContainer inv = this.golem.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slotStack = inv.getItem(i);
            if (slotStack.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slotStack, stack) && slotStack.getCount() + stack.getCount() <= slotStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean isFlower(ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.Identifier.withDefaultNamespace("small_flowers"))) || stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.Identifier.withDefaultNamespace("flowers")))) return true;
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
                ItemStack remainder = this.golem.getInventory().addItem(stack);
                
                if (remainder.isEmpty()) {
                    this.targetItem.discard();
                } else {
                    this.targetItem.setItem(remainder);
                }
                
                this.golem.setLastPickupTime(this.golem.level().getGameTime());
                this.golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, this.golem.getInventory().getItem(0).copy());
                
                this.golem.level().playSound(null, this.golem.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                this.targetItem = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetItem != null && this.targetItem.isAlive() && hasSpaceFor(this.targetItem.getItem());
    }

    @Override
    public void stop() {
        this.targetItem = null;
        this.golem.getNavigation().stop();
    }
}
