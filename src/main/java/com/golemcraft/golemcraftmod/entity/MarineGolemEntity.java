package com.golemcraft.golemcraftmod.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.golemcraft.golemcraftmod.client.sound.MarineGolemBeamSoundInstance;
import com.golemcraft.golemcraftmod.entity.ai.GolemCombatTargetGoals;
import com.golemcraft.golemcraftmod.entity.ai.StatueGoal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class MarineGolemEntity extends CompanionGolemEntity {

    public static final double SEA_LANTERN_Y_OFFSET = 1.375D;
    private static final double LASER_MIN_DIST_SQ = 12.25D;
    private static final double MELEE_MAX_DIST_SQ = 16.0D;

    private static final EntityDataAccessor<Integer> DATA_LASER_TARGET_ID =
            SynchedEntityData.defineId(MarineGolemEntity.class, EntityDataSerializers.INT);

    private int clientSideAttackTime;
    private @Nullable LivingEntity clientSideCachedLaserTarget;

    public MarineGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new AmphibiousMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LASER_TARGET_ID, 0);
    }

    @Override
    protected boolean emitsDynamicLight() {
        return true;
    }

    @Override
    public boolean shouldSyncSlot0ToMainHand() {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
        if (this.getMainHandItem().isEmpty()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TRIDENT));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        return data;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isVisuallySwimming() {
        return this.isInWater() && this.getOxidationLevel() < 3;
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        if (this.getOxidationLevel() == 3) {
            super.travelInWater(input, baseGravity, isFalling, oldY);
            return;
        }
        this.moveRelative(0.06F, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StatueGoal(this));
        this.goalSelector.addGoal(1, new MarineLaserAttackGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean canUse() {
                LivingEntity target = MarineGolemEntity.this.getTarget();
                if (target == null || !target.isAlive() || MarineGolemEntity.this.getOxidationLevel() == 3) {
                    return false;
                }
                double distSq = MarineGolemEntity.this.distanceToSqr(target);
                if (distSq > MELEE_MAX_DIST_SQ && MarineGolemEntity.this.hasLineOfSight(target)) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = MarineGolemEntity.this.getTarget();
                if (target == null || !target.isAlive() || MarineGolemEntity.this.getOxidationLevel() == 3) {
                    return false;
                }
                if (MarineGolemEntity.this.distanceToSqr(target) > MELEE_MAX_DIST_SQ && MarineGolemEntity.this.hasLineOfSight(target)) {
                    return false;
                }
                return super.canContinueToUse();
            }
        });
        this.registerCompanionGoals(3, 4, 6, false);
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new GolemCombatTargetGoals.OwnerHurtByTargetGoal(this, this::isStaying));
        this.targetSelector.addGoal(2, new GolemCombatTargetGoals.OwnerHurtTargetGoal(this, this::isStaying));
        this.targetSelector.addGoal(3, new GolemCombatTargetGoals.NearOwnerEnemyGoal(this, 20.0D, this::isStaying, this::getStayPos));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    public int getLaserAttackDuration() {
        return this.isCharged() ? 50 : 80;
    }

    public void setActiveLaserTarget(int entityId) {
        this.entityData.set(DATA_LASER_TARGET_ID, entityId);
    }

    public boolean hasActiveLaserTarget() {
        return this.entityData.get(DATA_LASER_TARGET_ID) != 0;
    }

    public @Nullable LivingEntity getActiveLaserTarget() {
        if (!this.hasActiveLaserTarget()) {
            return null;
        }
        if (this.level().isClientSide()) {
            if (this.clientSideCachedLaserTarget != null) {
                return this.clientSideCachedLaserTarget;
            }
            Entity entity = this.level().getEntity(this.entityData.get(DATA_LASER_TARGET_ID));
            if (entity instanceof LivingEntity living) {
                this.clientSideCachedLaserTarget = living;
                return living;
            }
            return null;
        }
        return this.getTarget();
    }

    public float getLaserAttackScale(float partialTick) {
        return (this.clientSideAttackTime + partialTick) / this.getLaserAttackDuration();
    }

    public float getClientSideAttackTime() {
        return this.clientSideAttackTime;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.setSwimming(this.isInWater() && this.getOxidationLevel() < 3);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_LASER_TARGET_ID.equals(key)) {
            this.clientSideAttackTime = 0;
            this.clientSideCachedLaserTarget = null;
            if (this.level().isClientSide() && this.hasActiveLaserTarget() && !this.isSilent()) {
                MarineGolemBeamSoundInstance.playFor(this);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.hasActiveLaserTarget()) {
            if (this.clientSideAttackTime < this.getLaserAttackDuration()) {
                this.clientSideAttackTime++;
            }
            LivingEntity target = this.getActiveLaserTarget();
            if (target != null) {
                this.getLookControl().setLookAt(target, 90.0F, 90.0F);
                this.getLookControl().tick();

                double scale = this.getLaserAttackScale(0.0F);
                double ox = this.getX();
                double oy = this.getY() + SEA_LANTERN_Y_OFFSET;
                double oz = this.getZ();
                double dx = target.getX() - ox;
                double dy = target.getY(0.5D) - oy;
                double dz = target.getZ() - oz;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 1.0E-4D) {
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    double step = this.random.nextDouble();
                    while (step < len) {
                        step += 1.8D - scale + this.random.nextDouble() * (1.7D - scale);
                        this.level().addParticle(
                                ParticleTypes.BUBBLE,
                                ox + dx * step,
                                oy + dy * step,
                                oz + dz * step,
                                0.0D, 0.0D, 0.0D
                        );
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.getOxidationLevel() == 3) return false;
        this.setAttackAnimTicks(10);
        boolean success = super.doHurtTarget(level, target);
        if (success) {
            this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
            if (this.isCharged() && target instanceof LivingEntity livingTarget) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.05D);
                this.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.8F);
                livingTarget.hurtServer(level, level.damageSources().lightningBolt(), 4.0F);
            }
            if (target instanceof Mob mobTarget) {
                mobTarget.setTarget(this);
            }
        }
        return success;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.level().isClientSide()) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !source.is(DamageTypes.THORNS) && !source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
            if (source.getDirectEntity() instanceof LivingEntity attacker && !this.isAlly(attacker)) {
                attacker.hurtServer(level, this.damageSources().thorns(this), 2.0F);
            }
        }
        return hurt;
    }

    private static class AmphibiousMoveControl extends MoveControl {
        private final MarineGolemEntity golem;

        AmphibiousMoveControl(MarineGolemEntity golem) {
            super(golem);
            this.golem = golem;
        }

        @Override
        public void tick() {
            if (this.golem.isInWater()) {
                LivingEntity target = this.golem.getTarget();
                if (target != null && target.getY() > this.golem.getY()) {
                    this.golem.setDeltaMovement(this.golem.getDeltaMovement().add(0.0D, 0.002D, 0.0D));
                }

                if (this.operation != MoveControl.Operation.MOVE_TO || this.golem.getNavigation().isDone()) {
                    this.golem.setSpeed(0.0F);
                    return;
                }

                double dx = this.wantedX - this.golem.getX();
                double dy = this.wantedY - this.golem.getY();
                double dz = this.wantedZ - this.golem.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 1.0E-5D) {
                    this.golem.setSpeed(0.0F);
                    return;
                }
                dy /= dist;
                float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                this.golem.setYRot(this.rotlerp(this.golem.getYRot(), targetYaw, 90.0F));
                this.golem.yBodyRot = this.golem.getYRot();

                float speed = (float) (this.speedModifier * this.golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.golem.setSpeed(Mth.lerp(0.125F, this.golem.getSpeed(), speed * 0.65F));
                this.golem.setDeltaMovement(this.golem.getDeltaMovement().add(
                        speed * dx * 0.005D,
                        speed * dy * 0.04D,
                        speed * dz * 0.005D
                ));
            } else {
                super.tick();
            }
        }
    }

    private static class MarineLaserAttackGoal extends Goal {
        private final MarineGolemEntity golem;
        private int attackTime;

        MarineLaserAttackGoal(MarineGolemEntity golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem.getTarget();
            return target != null
                    && target.isAlive()
                    && this.golem.getOxidationLevel() < 3
                    && this.golem.distanceToSqr(target) > LASER_MIN_DIST_SQ
                    && this.golem.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.attackTime = -10;
            this.golem.getNavigation().stop();
            LivingEntity target = this.golem.getTarget();
            if (target != null) {
                this.golem.getLookControl().setLookAt(target, 90.0F, 90.0F);
            }
        }

        @Override
        public void stop() {
            this.golem.setActiveLaserTarget(0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();
            if (target == null) return;

            this.golem.getNavigation().stop();
            this.golem.getLookControl().setLookAt(target, 90.0F, 90.0F);

            this.attackTime++;
            if (this.attackTime == 0) {
                this.golem.setActiveLaserTarget(target.getId());
            } else if (this.attackTime >= this.golem.getLaserAttackDuration()) {
                if (this.golem.level() instanceof ServerLevel serverLevel) {
                    float magicDamage = this.golem.level().getDifficulty() == Difficulty.HARD ? 3.0F : 1.0F;
                    target.hurtServer(serverLevel, this.golem.damageSources().indirectMagic(this.golem, this.golem), magicDamage);
                    target.hurtServer(serverLevel, this.golem.damageSources().mobAttack(this.golem), 6.0F);
                }
                this.attackTime = -10;
                this.golem.setActiveLaserTarget(0);
            }
        }
    }
}
