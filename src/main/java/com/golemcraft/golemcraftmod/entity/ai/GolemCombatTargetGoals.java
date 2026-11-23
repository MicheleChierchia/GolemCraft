package com.golemcraft.golemcraftmod.entity.ai;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GolemCombatTargetGoals {

    private GolemCombatTargetGoals() {}

    private static Player getOwner(BaseGolemEntity golem) {
        if (golem.getOwnerUUID() == null) return null;
        return golem.level().getPlayerByUUID(golem.getOwnerUUID());
    }

    public static class OwnerHurtByTargetGoal extends TargetGoal {
        private final BaseGolemEntity golem;
        private final BooleanSupplier isGuarding;
        private LivingEntity attackerOfOwner;
        private int lastHurtTs;

        public OwnerHurtByTargetGoal(BaseGolemEntity golem, BooleanSupplier isGuarding) {
            super(golem, false);
            this.golem = golem;
            this.isGuarding = isGuarding;
        }

        @Override
        public boolean canUse() {
            if (isGuarding.getAsBoolean()) return false;
            Player owner = getOwner(golem);
            if (owner == null) return false;
            attackerOfOwner = owner.getLastHurtByMob();
            int ts = owner.getLastHurtByMobTimestamp();
            return ts != lastHurtTs && attackerOfOwner != null && attackerOfOwner != mob
                    && canAttack(attackerOfOwner, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(attackerOfOwner);
            Player owner = getOwner(golem);
            if (owner != null) lastHurtTs = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    public static class OwnerHurtTargetGoal extends TargetGoal {
        private final BaseGolemEntity golem;
        private final BooleanSupplier isGuarding;
        private LivingEntity ownerTarget;
        private int lastHurtTs;

        public OwnerHurtTargetGoal(BaseGolemEntity golem, BooleanSupplier isGuarding) {
            super(golem, false);
            this.golem = golem;
            this.isGuarding = isGuarding;
        }

        @Override
        public boolean canUse() {
            if (isGuarding.getAsBoolean()) return false;
            Player owner = getOwner(golem);
            if (owner == null) return false;
            ownerTarget = owner.getLastHurtMob();
            int ts = owner.getLastHurtMobTimestamp();
            return ts != lastHurtTs && ownerTarget != null && ownerTarget != mob
                    && canAttack(ownerTarget, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(ownerTarget);
            Player owner = getOwner(golem);
            if (owner != null) lastHurtTs = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    public static class NearOwnerEnemyGoal extends TargetGoal {
        private final BaseGolemEntity golem;
        private final double range;
        private final BooleanSupplier isGuarding;
        private final Supplier<BlockPos> guardPosSupplier;
        private LivingEntity foundEnemy;

        public NearOwnerEnemyGoal(BaseGolemEntity golem, double range, BooleanSupplier isGuarding, Supplier<BlockPos> guardPosSupplier) {
            super(golem, false);
            this.golem = golem;
            this.range = range;
            this.isGuarding = isGuarding;
            this.guardPosSupplier = guardPosSupplier;
        }

        @Override
        public boolean canUse() {
            LivingEntity existing = mob.getTarget();
            if (existing != null && existing.isAlive()) return false;

            boolean guarding = isGuarding.getAsBoolean();
            BlockPos guardPos = guardPosSupplier.get();
            AABB searchBox;
            Vec3 center;

            if (guarding && guardPos != null) {
                searchBox = new AABB(guardPos).inflate(range);
                center = Vec3.atBottomCenterOf(guardPos);
            } else {
                Player owner = getOwner(golem);
                if (owner == null) return false;
                searchBox = owner.getBoundingBox().inflate(range);
                center = owner.position();
            }

            List<Mob> enemies = mob.level().getEntitiesOfClass(
                    Mob.class,
                    searchBox,
                    e -> e instanceof Enemy && e.isAlive() && !e.is(mob)
            );
            if (enemies.isEmpty()) return false;

            foundEnemy = enemies.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(center)))
                    .orElse(null);
            return foundEnemy != null && canAttack(foundEnemy, TargetingConditions.DEFAULT);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(foundEnemy);
            super.start();
        }
    }
}
