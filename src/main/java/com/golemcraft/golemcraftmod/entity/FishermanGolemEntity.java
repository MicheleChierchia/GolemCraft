package com.golemcraft.golemcraftmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class FishermanGolemEntity extends BaseGolemEntity {

    private static final EntityDataAccessor<Boolean> IS_FISHING = SynchedEntityData.defineId(FishermanGolemEntity.class, EntityDataSerializers.BOOLEAN);

    public FishermanGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_FISHING, false);
    }

    public boolean isFishing() {
        return this.entityData.get(IS_FISHING);
    }

    public void setFishing(boolean fishing) {
        this.entityData.set(IS_FISHING, fishing);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("IsFishing", com.mojang.serialization.Codec.BOOL, this.isFishing());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("IsFishing", com.mojang.serialization.Codec.BOOL).ifPresent(this::setFishing);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new com.golemcraft.golemcraftmod.entity.ai.EquipFishingRodGoal(this));
        this.goalSelector.addGoal(2, new com.golemcraft.golemcraftmod.entity.ai.DepositInChestGoal(this));
        this.goalSelector.addGoal(3, new com.golemcraft.golemcraftmod.entity.ai.FishermanFishGoal(this));
    }
}
