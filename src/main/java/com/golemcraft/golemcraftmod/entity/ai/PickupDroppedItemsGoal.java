package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class PickupDroppedItemsGoal extends Goal {
    private final BaseGolemEntity golem;
    private final double speed;
    private ItemEntity targetItem;
    private int searchCooldown;

    public PickupDroppedItemsGoal(BaseGolemEntity golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        if (isInventoryFull()) {
            return false;
        }

        if (golem.getOxidationLevel() == 3) {
            return false;
        }

        AABB searchBox = golem.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        List<ItemEntity> items = golem.level().getEntitiesOfClass(ItemEntity.class, searchBox, item -> {
            return item.isAlive() && !item.hasPickUpDelay();
        });

        if (!items.isEmpty()) {
            // Find closest
            double bestDist = Double.MAX_VALUE;
            for (ItemEntity item : items) {
                double dist = golem.distanceToSqr(item);
                if (dist < bestDist) {
                    bestDist = dist;
                    this.targetItem = item;
                }
            }
            return this.targetItem != null;
        }

        searchCooldown = 20; // 1 second cooldown if no items found
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null && targetItem.isAlive() && !targetItem.hasPickUpDelay() && !isInventoryFull();
    }

    @Override
    public void start() {
        golem.getNavigation().moveTo(targetItem, speed);
    }

    @Override
    public void tick() {
        if (targetItem == null) return;
        
        golem.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);

        if (golem.distanceToSqr(targetItem) < 4.0D) {
            ItemStack stack = targetItem.getItem();
            ItemStack remainder = golem.getInventory().addItem(stack);
            
            if (remainder.isEmpty()) {
                targetItem.discard();
                golem.setLastPickupTime(golem.level().getGameTime());
            } else {
                targetItem.setItem(remainder);
                if (stack.getCount() != remainder.getCount()) {
                    golem.setLastPickupTime(golem.level().getGameTime());
                }
            }
            
            golem.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.2F, (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F + 1.0F);
            targetItem = null;
        } else if (golem.getNavigation().isDone()) {
            golem.getNavigation().moveTo(targetItem, speed);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        golem.getNavigation().stop();
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            if (this.golem.getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
