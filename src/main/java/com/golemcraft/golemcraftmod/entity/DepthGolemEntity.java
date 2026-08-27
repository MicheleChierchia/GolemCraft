package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.entity.projectile.SonicBoomProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import java.util.function.BiConsumer;

import java.util.EnumSet;
import java.util.List;

public class DepthGolemEntity extends BaseGolemEntity implements VibrationSystem, VibrationSystem.User {

    private static final EntityDataAccessor<Boolean> IS_GUARDING = SynchedEntityData.defineId(DepthGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private BlockPos guardPos = null;
    private final VibrationSystem.Data vibrationData;
    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener;
    private LivingEntity heardEntity = null;
    private int heardEntityTime = 0;
    
    public int sonicAttackCooldown = 0;

    public DepthGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.vibrationData = new VibrationSystem.Data();
        this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_GUARDING, false);
    }

    public boolean isGuarding() { return this.entityData.get(IS_GUARDING); }
    public void setGuarding(boolean guarding) { this.entityData.set(IS_GUARDING, guarding); }
    public BlockPos getGuardPos() { return this.guardPos; }
    public void setGuardPos(BlockPos pos) { this.guardPos = pos; }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("IsGuarding", Codec.BOOL, this.isGuarding());
        if (this.guardPos != null) {
            output.store("GuardPos", BlockPos.CODEC, this.guardPos);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("IsGuarding", Codec.BOOL).ifPresent(this::setGuarding);
        input.read("GuardPos", BlockPos.CODEC).ifPresent(this::setGuardPos);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SonicAttackGoal(this));
        this.goalSelector.addGoal(2, new ReturnToGuardPositionGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.4D, 10.0F, 3.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HeardTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            VibrationSystem.Ticker.tick(serverLevel, this.getVibrationData(), this.getVibrationUser());
        }
        
        if (heardEntityTime > 0) {
            heardEntityTime--;
            if (heardEntityTime <= 0) {
                heardEntity = null;
            }
        }
        
        if (sonicAttackCooldown > 0) {
            sonicAttackCooldown--;
        }
    }

    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> p_218348_) {
        if (this.level() instanceof ServerLevel serverlevel) {
            p_218348_.accept(this.dynamicGameEventListener, serverlevel);
        }
    }

    private Player getOwnerPlayer() {
        if (this.getOwnerUUID() == null) return null;
        return this.level().getPlayerByUUID(this.getOwnerUUID());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).isEmpty() && player == this.getOwnerPlayer()) {
            if (!this.level().isClientSide()) {
                if (this.isGuarding()) {
                    this.setGuarding(false);
                    this.setGuardPos(null);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
                    }
                } else {
                    this.setGuarding(true);
                    this.setGuardPos(this.blockPosition());
                    this.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
                    }
                }
            }
            return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    // --- Vibration System ---

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this;
    }

    @Override
    public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, Entity sourceEntity, Entity projectileOwner, float distance) {
        if (sourceEntity instanceof LivingEntity living && living != this && living != this.getOwnerPlayer()) {
            if (living instanceof Enemy || (living instanceof Player p && p.getLastHurtMob() == this.getOwnerPlayer())) {
                this.heardEntity = living;
                this.heardEntityTime = 200; // 10 seconds memory
                this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context) {
        return true;
    }

    @Override
    public PositionSource getPositionSource() {
        return new EntityPositionSource(this, this.getEyeHeight());
    }

    @Override
    public int getListenerRadius() {
        return 16;
    }

    // --- AI Goals ---

    static class HeardTargetGoal extends TargetGoal {
        private final DepthGolemEntity golem;

        public HeardTargetGoal(DepthGolemEntity golem) {
            super(golem, false);
            this.golem = golem;
        }

        @Override
        public boolean canUse() {
            if (golem.heardEntity != null && golem.heardEntity.isAlive()) {
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            golem.setTarget(golem.heardEntity);
            super.start();
        }
    }

    static class SonicAttackGoal extends Goal {
        private final DepthGolemEntity golem;
        private LivingEntity target;
        private int attackTime;

        public SonicAttackGoal(DepthGolemEntity golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = golem.getTarget();
            return target != null && target.isAlive() && golem.sonicAttackCooldown <= 0;
        }

        @Override
        public void start() {
            this.attackTime = 20; // 1 second charge
            golem.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.0F);
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackTime > 0) {
                attackTime--;
                if (attackTime == 0) {
                    if (!golem.level().isClientSide()) {
                        SonicBoomProjectile proj = new SonicBoomProjectile(golem.level(), golem);
                        
                        double d0 = target.getX() - golem.getX();
                        double d1 = target.getY(0.5D) - proj.getY();
                        double d2 = target.getZ() - golem.getZ();
                        
                        proj.shoot(d0, d1, d2, 1.5F, 1.0F);
                        golem.level().addFreshEntity(proj);
                        golem.playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 1.0F);
                        golem.sonicAttackCooldown = 60; // 3 seconds cooldown
                    }
                }
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return attackTime > 0 && target != null && target.isAlive();
        }
    }

    static class ReturnToGuardPositionGoal extends Goal {
        private final DepthGolemEntity golem;
        private final double speedModifier;

        public ReturnToGuardPositionGoal(DepthGolemEntity golem, double speed) {
            this.golem = golem;
            this.speedModifier = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!golem.isGuarding() || golem.getGuardPos() == null) return false;
            if (golem.getTarget() != null) return false; // combat takes priority
            return golem.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(golem.getGuardPos())) > 25.0D; // 5 blocks
        }

        @Override
        public void start() {
            golem.getNavigation().moveTo(golem.getGuardPos().getX() + 0.5, golem.getGuardPos().getY(), golem.getGuardPos().getZ() + 0.5, speedModifier);
        }

        @Override
        public boolean canContinueToUse() {
            return !golem.getNavigation().isDone() && golem.getTarget() == null && golem.isGuarding();
        }
    }

    static class FollowOwnerGoal extends Goal {
        private final DepthGolemEntity golem;
        private final double speed;
        private final float startDistSq;
        private final float stopDistSq;
        private Player owner;
        private int recalcTimer;

        FollowOwnerGoal(DepthGolemEntity golem, double speed, float startDist, float stopDist) {
            this.golem = golem;
            this.speed = speed;
            this.startDistSq = startDist * startDist;
            this.stopDistSq = stopDist * stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.isGuarding()) return false;
            owner = golem.getOwnerPlayer();
            if (owner == null) return false;
            LivingEntity t = golem.getTarget();
            if (t != null && t.isAlive()) return false; // in combat
            return golem.distanceToSqr(owner) >= startDistSq;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.isGuarding()) return false;
            owner = golem.getOwnerPlayer();
            if (owner == null || !owner.isAlive()) return false;
            LivingEntity t = golem.getTarget();
            if (t != null && t.isAlive()) return false;
            return golem.distanceToSqr(owner) > stopDistSq;
        }

        @Override
        public void start() { recalcTimer = 0; }

        @Override
        public void stop() {
            golem.getNavigation().stop();
            owner = null;
        }

        @Override
        public void tick() {
            owner = golem.getOwnerPlayer();
            if (owner == null) return;

            golem.getLookControl().setLookAt(owner, 10.0F, golem.getMaxHeadXRot());

            if (--recalcTimer > 0) return;
            recalcTimer = 10;

            double distSq = golem.distanceToSqr(owner);
            if (distSq <= stopDistSq) { 
                golem.getNavigation().stop(); 
                return; 
            }

            if (distSq > 144.0D) {
                teleportNear(owner);
            } else {
                golem.getNavigation().moveTo(owner, speed);
            }
        }

        private void teleportNear(Player target) {
            net.minecraft.util.RandomSource rng = golem.getRandom();
            for (int i = 0; i < 10; i++) {
                double ox = target.getX() + (rng.nextFloat() * 6.0F - 3.0F);
                double oz = target.getZ() + (rng.nextFloat() * 6.0F - 3.0F);
                BlockPos g = BlockPos.containing(ox, target.getY() + 2, oz);
                int tries = 6;
                while (tries-- > 0 && !golem.level().getBlockState(g).isSolid()) {
                    g = g.below();
                }
                
                if (golem.level().getBlockState(g).isSolid()) {
                    BlockPos tpPos = g.above();
                    if (golem.level().noCollision(golem, golem.getBoundingBox().move(
                            ox - golem.getX(), tpPos.getY() - golem.getY(), oz - golem.getZ()))) {
                        golem.teleportTo(ox, tpPos.getY(), oz);
                        golem.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }
}
