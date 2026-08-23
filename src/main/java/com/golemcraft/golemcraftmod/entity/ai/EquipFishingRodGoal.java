package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.FishermanGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FishingRodItem;

public class EquipFishingRodGoal extends Goal {
    private final FishermanGolemEntity golem;

    public EquipFishingRodGoal(FishermanGolemEntity golem) {
        this.golem = golem;
    }

    @Override
    public boolean canUse() {
        ItemStack handItem = this.golem.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (handItem.getItem() instanceof FishingRodItem) {
            return false;
        }

        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (stack.getItem() instanceof FishingRodItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (stack.getItem() instanceof FishingRodItem) {
                ItemStack handItem = this.golem.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
                this.golem.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
                this.golem.getInventory().setItem(i, handItem.copy());
                break;
            }
        }
    }
}
