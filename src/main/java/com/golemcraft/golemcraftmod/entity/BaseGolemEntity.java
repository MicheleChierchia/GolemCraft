package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;

public class BaseGolemEntity extends PathfinderMob implements ContainerUser {
    private static final EntityDataAccessor<Boolean> RUMMAGING = SynchedEntityData.defineId(BaseGolemEntity.class, EntityDataSerializers.BOOLEAN);
    
    private final SimpleContainer inventory = new SimpleContainer(1);

    public BaseGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
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
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public double getContainerInteractionRange() {
        return 8.0D;
    }

    @Override
    public boolean hasContainerOpen(net.minecraft.world.level.block.entity.ContainerOpenersCounter openersCounter, net.minecraft.core.BlockPos pos) {
        return this.isRummaging(); 
    }

    @Override
    public LivingEntity getLivingEntity() {
        return this;
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
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        // Transform logic
        if (itemstack.is(net.minecraft.tags.ItemTags.SMALL_FLOWERS) || itemstack.is(net.minecraft.tags.ItemTags.FLOWERS) || (itemstack.getItem() instanceof net.minecraft.world.item.BlockItem bi && (bi.getBlock() instanceof net.minecraft.world.level.block.FlowerBlock || bi.getBlock() instanceof net.minecraft.world.level.block.TallFlowerBlock))) {
            if (!this.level().isClientSide()) {
                FlowerGolemEntity flowerGolem = ModEntities.FLOWER_GOLEM.get().create(this.level(), net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
                if (flowerGolem != null) {
                    flowerGolem.setPos(this.getX(), this.getY(), this.getZ());
                    flowerGolem.setYRot(this.getYRot());
                    flowerGolem.setXRot(this.getXRot());
                    flowerGolem.setHealth(this.getHealth());
                    flowerGolem.yBodyRot = this.yBodyRot;
                    if (this.hasCustomName()) {
                        flowerGolem.setCustomName(this.getCustomName());
                        flowerGolem.setCustomNameVisible(this.isCustomNameVisible());
                    }
                    this.level().addFreshEntity(flowerGolem);
                    
                    // Particles and sounds
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.0D);
                    this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
                    
                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                    this.discard();
                }
            }
            return InteractionResult.SUCCESS;
        }
        
        return super.mobInteract(player, hand);
    }
}
