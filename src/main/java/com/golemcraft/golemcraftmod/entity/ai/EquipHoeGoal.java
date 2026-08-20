package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.FarmerGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import java.util.EnumSet;

public class EquipHoeGoal extends Goal {
    private final FarmerGolemEntity golem;

    public EquipHoeGoal(FarmerGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!this.golem.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return false;
        }
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
                if (this.golem.getInventory().getItem(i).getItem() instanceof HoeItem) {
                    return true;
                }
            }
        return false;
    }

    @Override
    public void start() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (stack.getItem() instanceof HoeItem) {
                this.golem.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                this.golem.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
                this.golem.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }
    }
}
