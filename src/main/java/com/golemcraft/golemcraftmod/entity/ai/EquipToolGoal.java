package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EquipToolGoal extends Goal {
    private final BaseGolemEntity golem;
    private final Predicate<ItemStack> toolPredicate;

    public EquipToolGoal(BaseGolemEntity golem, Predicate<ItemStack> toolPredicate) {
        this.golem = golem;
        this.toolPredicate = toolPredicate;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!this.golem.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return false;
        }
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            if (this.toolPredicate.test(this.golem.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (this.toolPredicate.test(stack)) {
                ItemStack handItem = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
                this.golem.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                this.golem.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                this.golem.getInventory().setItem(i, handItem.copy());
                break;
            }
        }
    }
}
