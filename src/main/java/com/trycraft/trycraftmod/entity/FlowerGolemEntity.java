package com.trycraft.trycraftmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.entity.LivingEntity;

public class FlowerGolemEntity extends PathfinderMob implements ContainerUser {
    private static final EntityDataAccessor<Boolean> RUMMAGING = SynchedEntityData.defineId(FlowerGolemEntity.class, EntityDataSerializers.BOOLEAN);
    
    @Override
    public double getContainerInteractionRange() {
        return 8.0D;
    }

    @Override
    public boolean hasContainerOpen(net.minecraft.world.level.block.entity.ContainerOpenersCounter openersCounter, net.minecraft.core.BlockPos pos) {
        return this.isRummaging(); // Mantieni aperta se sta rovistando
    }

    @Override
    public LivingEntity getLivingEntity() {
        return this;
    }
    private final SimpleContainer inventory = new SimpleContainer(1);
    
    public FlowerGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RUMMAGING, false);
    }

    public void setRummaging(boolean rummaging) {
        this.entityData.set(RUMMAGING, rummaging);
    }

    public boolean isRummaging() {
        return this.entityData.get(RUMMAGING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.trycraft.trycraftmod.entity.ai.HarvestPlantedFlowerGoal(this));
        this.goalSelector.addGoal(2, new com.trycraft.trycraftmod.entity.ai.PickupFlowerGoal(this));
        this.goalSelector.addGoal(3, new com.trycraft.trycraftmod.entity.ai.DepositInChestGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.world.level.block.SoundType.COPPER.getStepSound(); 
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return net.minecraft.world.level.block.SoundType.COPPER.getHitSound();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.world.level.block.SoundType.COPPER.getBreakSound();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState blockState) {
        this.playSound(net.minecraft.world.level.block.SoundType.COPPER.getStepSound(), 0.15F, 1.0F);
    }
}
