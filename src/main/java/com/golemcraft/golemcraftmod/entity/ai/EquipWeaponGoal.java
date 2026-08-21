package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.SoldierGolemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Makes the Soldier Golem search its inventory for a usable weapon whenever
 * its main hand is empty (e.g. the previous weapon broke), and equip the first
 * one it finds — removing it from the inventory in the process.
 */
public class EquipWeaponGoal extends Goal {

    private final SoldierGolemEntity golem;

    public EquipWeaponGoal(SoldierGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class)); // background goal, no flags needed
    }

    @Override
    public boolean canUse() {
        // Only trigger when the main hand is truly empty
        if (!this.golem.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return false;
        }
        // Check if any slot in the inventory holds a usable weapon
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            if (SoldierGolemEntity.isWeapon(this.golem.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (SoldierGolemEntity.isWeapon(stack)) {
                this.golem.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                this.golem.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                this.golem.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }
    }
}
