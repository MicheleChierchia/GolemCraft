package com.golemcraft.golemcraftmod.entity.projectile;

import com.golemcraft.golemcraftmod.entity.DepthGolemEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SonicBoomProjectile extends Projectile {

    public SonicBoomProjectile(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public SonicBoomProjectile(Level level, DepthGolemEntity owner) {
        super(com.golemcraft.golemcraftmod.registry.ModEntities.SONIC_BOOM_PROJECTILE.get(), level);
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
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        this.setPos(d0, d1, d2);

        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();
        
        if (entity == owner) return;
        
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.hurt(this.damageSources().sonicBoom(this.getOwner()), 10.0F); // Sonic boom damage ignores armor
            
            double d0 = 0.5D * (1.0D - livingEntity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            double d1 = 2.5D * (1.0D - livingEntity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            livingEntity.push(this.getDeltaMovement().x * d1, this.getDeltaMovement().y * d0, this.getDeltaMovement().z * d1);
        }
        
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
