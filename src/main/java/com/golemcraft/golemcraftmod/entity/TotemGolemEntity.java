package com.golemcraft.golemcraftmod.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TotemGolemEntity extends BaseGolemEntity {

    private static final EntityDataAccessor<Boolean> IS_STAYING =
            SynchedEntityData.defineId(TotemGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private BlockPos stayPos = null;

    public TotemGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_STAYING, false);
    }

    public boolean isStaying() {
        return this.entityData.get(IS_STAYING);
    }

    public void setStaying(boolean staying) {
        this.entityData.set(IS_STAYING, staying);
    }

    public BlockPos getStayPos() {
        return this.stayPos;
    }

    public void setStayPos(BlockPos pos) {
        this.stayPos = pos;
    }

    public Player getOwnerPlayer() {
        if (this.getOwnerUUID() == null) return null;
        return this.level().getPlayerByUUID(this.getOwnerUUID());
    }

    @Override
    public int getOxidationLevel() {
        // Gold / Totem does not oxidize
        return 0;
    }

    @Override
    public void setOxidationLevel(int level) {
        super.setOxidationLevel(0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ReturnToStayPositionGoal(this, 1.1D));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.35D, 7.0F, 2.5F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                if (TotemGolemEntity.this.isStaying()) return false;
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) {
            return result;
        }

        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.isEmpty() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID())) {
            if (!this.level().isClientSide()) {
                boolean nextState = !this.isStaying();
                this.setStaying(nextState);
                if (nextState) {
                    this.setStayPos(this.blockPosition());
                    this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.0F);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.8D, this.getZ(), 6, 0.25D, 0.25D, 0.25D, 0.0D);
                    }
                } else {
                    this.setStayPos(null);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.9F);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.8D, this.getZ(), 4, 0.25D, 0.25D, 0.25D, 0.0D);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Called when this golem sacrifices itself to revive the player.
     */
    public void triggerTotemRescue(ServerPlayer player) {
        if (this.level().isClientSide()) return;

        Level level = this.level();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 0.7D, this.getZ(), 45, 0.4D, 0.5D, 0.4D, 0.15D);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TOTEM_USE, this.getSoundSource(), 1.0F, 1.0F);

            // Revert into BaseGolem
            com.golemcraft.golemcraftmod.entity.BaseGolemEntity baseGolem =
                    com.golemcraft.golemcraftmod.registry.ModEntities.BASE_GOLEM.get().create(sl, EntitySpawnReason.CONVERSION);
            if (baseGolem != null) {
                this.copyBaseDataTo(baseGolem, player);
                sl.addFreshEntity(baseGolem);
            }
        }

        this.discard();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("IsStaying", com.mojang.serialization.Codec.BOOL, this.isStaying());
        if (this.stayPos != null) {
            output.store("StayPos", BlockPos.CODEC, this.stayPos);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("IsStaying", com.mojang.serialization.Codec.BOOL).ifPresent(this::setStaying);
        input.read("StayPos", BlockPos.CODEC).ifPresent(this::setStayPos);
    }

    // =========================================================================
    //  AI Goals
    // =========================================================================

    static class ReturnToStayPositionGoal extends Goal {
        private final TotemGolemEntity golem;
        private final double speed;

        ReturnToStayPositionGoal(TotemGolemEntity golem, double speed) {
            this.golem = golem;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!golem.isStaying() || golem.getStayPos() == null) return false;
            return golem.distanceToSqr(Vec3.atBottomCenterOf(golem.getStayPos())) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            if (!golem.isStaying() || golem.getStayPos() == null) return false;
            return !golem.getNavigation().isDone() && golem.distanceToSqr(Vec3.atBottomCenterOf(golem.getStayPos())) > 1.5D;
        }

        @Override
        public void start() {
            if (golem.getStayPos() != null) {
                golem.getNavigation().moveTo(golem.getStayPos().getX() + 0.5D,
                        golem.getStayPos().getY(),
                        golem.getStayPos().getZ() + 0.5D, speed);
            }
        }
    }

    static class FollowOwnerGoal extends Goal {
        private final TotemGolemEntity golem;
        private final double speed;
        private final float startDistSq;
        private final float stopDistSq;
        private Player owner;
        private int recalcTimer;

        FollowOwnerGoal(TotemGolemEntity golem, double speed, float startDist, float stopDist) {
            this.golem = golem;
            this.speed = speed;
            this.startDistSq = startDist * startDist;
            this.stopDistSq = stopDist * stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.isStaying()) return false;
            owner = golem.getOwnerPlayer();
            if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;
            return golem.distanceToSqr(owner) >= startDistSq;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.isStaying()) return false;
            owner = golem.getOwnerPlayer();
            if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;
            return golem.distanceToSqr(owner) > stopDistSq;
        }

        @Override
        public void start() {
            recalcTimer = 0;
        }

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
            RandomSource rng = golem.getRandom();
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
