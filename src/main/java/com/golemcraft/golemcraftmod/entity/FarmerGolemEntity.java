package com.golemcraft.golemcraftmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.network.chat.Component;

public class FarmerGolemEntity extends BaseGolemEntity {

    public FarmerGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new com.golemcraft.golemcraftmod.entity.ai.FarmerHarvestGoal(this));
        this.goalSelector.addGoal(2, new com.golemcraft.golemcraftmod.entity.ai.FarmerPlantGoal(this));
        this.goalSelector.addGoal(3, new com.golemcraft.golemcraftmod.entity.ai.FarmerTillGoal(this));
        this.goalSelector.addGoal(4, new com.golemcraft.golemcraftmod.entity.ai.DepositInChestGoal(this));
        this.goalSelector.addGoal(5, new com.golemcraft.golemcraftmod.entity.ai.EquipHoeGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }
    

}
