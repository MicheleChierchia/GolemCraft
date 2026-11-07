package com.golemcraft.golemcraftmod.entity.projectile;

import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class HoneySpitProjectile extends Projectile {

    public HoneySpitProjectile(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public HoneySpitProjectile(Level level, LivingEntity owner) {
        super(ModEntities.HONEY_SPIT_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        Vec3 vec3 = this.getDeltaMovement();
        double d0 = this.getX() + vec3.x;
        double d1 = this.getY() + vec3.y;
        double d2 = this.getZ() + vec3.z;
        this.updateRotation();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FALLING_HONEY, this.getX(), this.getY(), this.getZ(), 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }

        this.setPos(d0, d1, d2);

        if (this.tickCount > 80) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (target == this.getOwner()) return false;
        // User rule: never target or hit creepers
        if (target instanceof Creeper) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();

        if (entity == owner) return;

        if (entity instanceof Bee bee) {
            if (bee.isAngry()) {
                bee.stopBeingAngry();
                bee.setTarget(null);
            }
            bee.heal(4.0F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, bee.getX(), bee.getY() + 0.3D, bee.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.LANDING_HONEY, bee.getX(), bee.getY() + 0.3D, bee.getZ(), 8, 0.1D, 0.1D, 0.1D, 0.02D);
            }
            this.level().playSound(null, bee.getX(), bee.getY(), bee.getZ(), SoundEvents.HONEY_BLOCK_FALL, SoundSource.NEUTRAL, 1.0F, 1.2F);
        } else if (entity instanceof LivingEntity livingEntity && !(entity instanceof Creeper)) {
            // Apply slight damage and sticky slowness to hostile monsters
            livingEntity.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity livingOwner ? livingOwner : null), 2.0F);
            livingEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LANDING_HONEY, livingEntity.getX(), livingEntity.getY() + 0.5D, livingEntity.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            this.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.NEUTRAL, 0.8F, 1.0F);
        }

        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LANDING_HONEY, this.getX(), this.getY(), this.getZ(), 6, 0.1D, 0.1D, 0.1D, 0.02D);
            }
            this.discard();
        }
    }
}
