package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BeekeeperGolemEntity;
import com.golemcraft.golemcraftmod.entity.projectile.HoneySpitProjectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class BeekeeperShootHoneyGoal extends Goal {
    private final BeekeeperGolemEntity golem;
    private LivingEntity target;
    private int cooldown = 0;

    public BeekeeperShootHoneyGoal(BeekeeperGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.cooldown-- > 0) return false;

        AABB box = this.golem.getBoundingBox().inflate(12.0D, 4.0D, 12.0D);
        List<LivingEntity> nearby = this.golem.level().getEntitiesOfClass(LivingEntity.class, box);

        // Priority 1: Bees that are angry or injured
        for (LivingEntity e : nearby) {
            if (e instanceof Bee bee && (bee.isAngry() || bee.getHealth() < bee.getMaxHealth())) {
                this.target = bee;
                return true;
            }
        }

        // Priority 2: Hostile monsters nearby (except Creepers)
        for (LivingEntity e : nearby) {
            if (e instanceof Enemy && !(e instanceof Creeper) && e.isAlive() && this.golem.hasLineOfSight(e)) {
                this.target = e;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive() && !(this.target instanceof Creeper) && this.golem.distanceToSqr(this.target) <= 144.0D;
    }

    @Override
    public void start() {
        this.cooldown = 30;
    }

    @Override
    public void tick() {
        if (this.target == null) return;
        this.golem.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        double distSq = this.golem.distanceToSqr(this.target);
        if (distSq <= 144.0D) {
            // Shoot honey projectile
            Vec3 golemEye = this.golem.getEyePosition();
            Vec3 targetCenter = this.target.getBoundingBox().getCenter();
            Vec3 dir = targetCenter.subtract(golemEye).normalize();

            HoneySpitProjectile projectile = new HoneySpitProjectile(this.golem.level(), this.golem);
            projectile.setDeltaMovement(dir.scale(1.2D));
            this.golem.level().addFreshEntity(projectile);

            this.golem.level().playSound(null, this.golem.getX(), this.golem.getY(), this.golem.getZ(),
                    SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, 1.0F, 1.0F + (this.golem.getRandom().nextFloat() - 0.5F) * 0.2F);

            this.golem.setAttackAnimTicks(10);
            this.cooldown = 40;
            this.target = null;
        }
    }

    @Override
    public void stop() {
        this.target = null;
    }
}
