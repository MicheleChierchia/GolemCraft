package com.golemcraft.golemcraftmod.entity.projectile;

import com.golemcraft.golemcraftmod.entity.FishermanGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class GolemFishingHook extends Projectile {
    private static final EntityDataAccessor<Boolean> DATA_BITING = SynchedEntityData.defineId(GolemFishingHook.class, EntityDataSerializers.BOOLEAN);
    
    private FishermanGolemEntity golemOwner;
    private int life;
    private int timeUntilLured;
    private int timeUntilHooked;
    private int nibble;
    private float fishAngle;
    private boolean biting;
    private State currentState = State.FLYING;

    public GolemFishingHook(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public GolemFishingHook(FishermanGolemEntity golem, Level level, BlockPos targetWaterPos) {
        super(com.golemcraft.golemcraftmod.registry.ModEntities.GOLEM_FISHING_HOOK.get(), level);
        this.setOwner(golem);
        this.golemOwner = golem;

        float f = golem.getXRot();
        float f1 = golem.getYRot();
        float f2 = Mth.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = Mth.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        
        double d0 = golem.getX() - (double)f3 * 0.3D;
        double d1 = golem.getEyeY();
        double d2 = golem.getZ() - (double)f2 * 0.3D;
        this.setPos(d0, d1, d2);
        
        // Calculate velocity towards the target water block
        double targetX = targetWaterPos.getX() + 0.5D - d0;
        double targetY = targetWaterPos.getY() + 0.5D - d1;
        double targetZ = targetWaterPos.getZ() + 0.5D - d2;
        
        Vec3 vec3 = new Vec3(targetX, targetY, targetZ);
        double dist = vec3.horizontalDistance();
        // Add a bit of arc (upward Y velocity based on distance)
        vec3 = new Vec3(targetX, targetY + dist * 0.2D, targetZ).normalize();
        
        // Scale velocity based on distance to ensure it reaches (roughly)
        double speed = Math.min(dist * 0.15D + 0.5D, 1.5D);
        vec3 = vec3.scale(speed);
        
        this.setDeltaMovement(vec3);
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(Mth.atan2(vec3.y, vec3.horizontalDistance()) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BITING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_37153_) {
        if (DATA_BITING.equals(p_37153_)) {
            this.biting = this.getEntityData().get(DATA_BITING);
            if (this.biting) {
                this.setDeltaMovement(this.getDeltaMovement().x, -0.4F * Mth.nextFloat(this.random, 0.6F, 1.0F), this.getDeltaMovement().z);
            }
        }
        super.onSyncedDataUpdated(p_37153_);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.golemOwner == null && this.getOwner() instanceof FishermanGolemEntity golem) {
            this.golemOwner = golem;
        }

        if (this.golemOwner == null || this.golemOwner.isRemoved() || !this.golemOwner.isAlive() || this.distanceToSqr(this.golemOwner) > 1024.0D) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide()) {
            if (!this.golemOwner.isFishing() || !(this.golemOwner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem() instanceof net.minecraft.world.item.FishingRodItem)) {
                this.discard();
                return;
            }
        }

        if (this.onGround()) {
            this.life++;
            if (this.life >= 1200) {
                this.discard();
                return;
            }
        } else {
            this.life = 0;
        }

        float f = 0.0F;
        BlockPos blockpos = this.blockPosition();
        FluidState fluidstate = this.level().getFluidState(blockpos);
        if (fluidstate.is(FluidTags.WATER)) {
            f = fluidstate.getHeight(this.level(), blockpos);
        }
        boolean inWater = f > 0.0F;

        if (this.currentState == State.FLYING) {
            if (inWater) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
                this.currentState = State.BOBBING;
                return;
            }
        } else if (this.currentState == State.BOBBING) {
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = this.getY() + vec3.y - (double)blockpos.getY() - (double)f;
            if (Math.abs(d0) < 0.01D) d0 += Math.signum(d0) * 0.1D;
            
            this.setDeltaMovement(vec3.x * 0.9D, vec3.y - d0 * (double)this.random.nextFloat() * 0.2D, vec3.z * 0.9D);

            if (inWater && !this.level().isClientSide()) {
                this.catchingFish(blockpos);
            }
        }

        if (!fluidstate.is(FluidTags.WATER) && !this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateRotation();
        
        if (this.currentState == State.FLYING && (this.onGround() || this.horizontalCollision)) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.setDeltaMovement(this.getDeltaMovement().scale(0.92D));
        this.reapplyPosition();
    }

    private void catchingFish(BlockPos pos) {
        ServerLevel serverlevel = (ServerLevel)this.level();
        int i = 1; // lure speed equivalent

        if (this.nibble > 0) {
            this.nibble--;
            if (this.nibble <= 0) {
                this.timeUntilLured = 0;
                this.timeUntilHooked = 0;
                this.getEntityData().set(DATA_BITING, false);
            }
        } else if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= i;
            if (this.timeUntilHooked > 0) {
                this.fishAngle = this.fishAngle + (float)this.random.triangle(0.0D, 9.188D);
                float f1 = Mth.sin(this.fishAngle * ((float)Math.PI / 180F));
                float f2 = Mth.cos(this.fishAngle * ((float)Math.PI / 180F));
                double d0 = this.getX() + (double)(f1 * (float)this.timeUntilHooked * 0.1F);
                double d1 = (double)((float)Mth.floor(this.getY()) + 1.0F);
                double d2 = this.getZ() + (double)(f2 * (float)this.timeUntilHooked * 0.1F);
                BlockState blockstate = serverlevel.getBlockState(BlockPos.containing(d0, d1 - 1.0D, d2));
                
                if (blockstate.is(Blocks.WATER)) {
                    if (this.random.nextFloat() < 0.15F) {
                        serverlevel.sendParticles(ParticleTypes.BUBBLE, d0, d1 - 0.1D, d2, 1, (double)f1, 0.1D, (double)f2, 0.0D);
                    }
                    float f3 = f1 * 0.04F;
                    float f4 = f2 * 0.04F;
                    serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)f4, 0.01D, (double)(-f3), 1.0D);
                    serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)(-f4), 0.01D, (double)f3, 1.0D);
                }
            } else {
                this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
                double d3 = this.getY() + 0.5D;
                serverlevel.sendParticles(ParticleTypes.BUBBLE, this.getX(), d3, this.getZ(), (int)(1.0F + this.getBbWidth() * 20.0F), (double)this.getBbWidth(), 0.0D, (double)this.getBbWidth(), 0.2D);
                serverlevel.sendParticles(ParticleTypes.FISHING, this.getX(), d3, this.getZ(), (int)(1.0F + this.getBbWidth() * 20.0F), (double)this.getBbWidth(), 0.0D, (double)this.getBbWidth(), 0.2D);
                this.nibble = Mth.nextInt(this.random, 20, 40);
                this.getEntityData().set(DATA_BITING, true);
            }
        } else if (this.timeUntilLured > 0) {
            this.timeUntilLured -= i;
            if (this.timeUntilLured <= 0) {
                this.fishAngle = Mth.nextFloat(this.random, 0.0F, 360.0F);
                this.timeUntilHooked = Mth.nextInt(this.random, 20, 80);
            }
        } else {
            // Speed up catching for the golem: 100 to 300 ticks (5 to 15 seconds)
            this.timeUntilLured = Mth.nextInt(this.random, 100, 300);
            if (this.golemOwner != null) {
                ItemStack rod = this.golemOwner.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                try {
                    var registry = this.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                    var lure = registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LURE);
                    int lureLevel = rod.getEnchantmentLevel(lure);
                    this.timeUntilLured -= lureLevel * 100;
                    if (this.timeUntilLured < 20) this.timeUntilLured = 20;
                } catch (Exception e) {}
            }
        }
    }

    public boolean isBiting() {
        return this.nibble > 0;
    }

    public FishermanGolemEntity getGolemOwner() {
        return this.golemOwner;
    }

    public void retrieve() {
        this.discard();
    }

    enum State {
        FLYING,
        BOBBING
    }
}
