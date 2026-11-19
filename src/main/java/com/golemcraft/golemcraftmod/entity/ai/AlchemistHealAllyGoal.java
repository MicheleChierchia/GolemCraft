package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.AlchemistGolemEntity;
import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

/**
 * Throws Splash Potions of Healing at injured allies, prioritizing the owner player.
 */
public class AlchemistHealAllyGoal extends Goal {

    private static final double HEAL_RANGE = 12.0D;
    private static final double IDEAL_THROW_RANGE_SQ = 64.0D;
    private static final double MAX_RANGE_SQ = HEAL_RANGE * HEAL_RANGE;
    private static final float HEAL_THRESHOLD_RATIO = 0.75F;
    private static final int THROW_WINDUP_TICKS = 10;
    private static final int NORMAL_COOLDOWN = 80;
    private static final int CHARGED_COOLDOWN = 40;

    private final AlchemistGolemEntity golem;
    private LivingEntity target;
    private int cooldown = 0;
    private int windupTicks = -1;
    private int repathTimer = 0;

    public AlchemistHealAllyGoal(AlchemistGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.getOxidationLevel() == 3 || golem.isStaying() || golem.isDrinkingPotion() || golem.getOwnerUUID() == null) {
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.target = this.findBestTarget();
        return this.target != null;
    }

    private LivingEntity findBestTarget() {
        Player owner = golem.getOwnerPlayer();
        if (isHealableOwner(owner)) {
            return owner;
        }

        List<LivingEntity> candidates = golem.level().getEntitiesOfClass(
                LivingEntity.class,
                golem.getBoundingBox().inflate(HEAL_RANGE, 6.0D, HEAL_RANGE),
                this::isHealableOtherAlly
        );

        LivingEntity best = null;
        float bestRatio = Float.MAX_VALUE;
        double bestDistSq = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            float ratio = candidate.getHealth() / candidate.getMaxHealth();
            double distSq = golem.distanceToSqr(candidate);
            if (ratio < bestRatio || (ratio == bestRatio && distSq < bestDistSq)) {
                best = candidate;
                bestRatio = ratio;
                bestDistSq = distSq;
            }
        }
        return best;
    }

    private boolean isHealableOwner(Player owner) {
        return owner != null
                && owner.isAlive()
                && !owner.isSpectator()
                && !owner.isCreative()
                && owner.isAffectedByPotions()
                && !owner.isInvertedHealAndHarm()
                && owner.getHealth() <= owner.getMaxHealth() * HEAL_THRESHOLD_RATIO
                && golem.distanceToSqr(owner) <= MAX_RANGE_SQ;
    }

    private boolean isHealableOtherAlly(LivingEntity entity) {
        if (entity == golem || !entity.isAlive() || !entity.isAffectedByPotions() || entity.isInvertedHealAndHarm()) {
            return false;
        }
        if (!golem.isAlly(entity)) {
            return false;
        }
        if (entity instanceof BaseGolemEntity otherGolem && otherGolem.getOxidationLevel() == 3) {
            return false;
        }
        return entity.getHealth() <= entity.getMaxHealth() * HEAL_THRESHOLD_RATIO
                && golem.distanceToSqr(entity) <= MAX_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (golem.getOxidationLevel() == 3 || golem.isStaying() || golem.isDrinkingPotion()) {
            return false;
        }
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (target.getHealth() >= target.getMaxHealth() || golem.distanceToSqr(target) > MAX_RANGE_SQ) {
            return false;
        }
        if (target instanceof Player p && (p.isSpectator() || p.isCreative())) {
            return false;
        }
        return this.cooldown <= 0;
    }

    @Override
    public void start() {
        this.windupTicks = -1;
        this.repathTimer = 0;
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        this.target = null;
        this.windupTicks = -1;
        if (golem.getPotionAction() == AlchemistGolemEntity.POTION_THROWING_HEALING) {
            golem.setPotionAction(AlchemistGolemEntity.POTION_NONE);
        }
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        if (!(this.target instanceof Player)) {
            Player owner = golem.getOwnerPlayer();
            if (isHealableOwner(owner)) {
                this.target = owner;
            }
        }

        golem.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        double distSq = golem.distanceToSqr(this.target);
        boolean hasLos = golem.getSensing().hasLineOfSight(this.target);

        if (this.windupTicks < 0) {
            if (distSq > IDEAL_THROW_RANGE_SQ || !hasLos) {
                if (--this.repathTimer <= 0) {
                    this.repathTimer = 10;
                    golem.getNavigation().moveTo(this.target, 1.15D);
                }
            } else {
                golem.getNavigation().stop();
                this.windupTicks = THROW_WINDUP_TICKS;
                golem.setPotionAction(AlchemistGolemEntity.POTION_THROWING_HEALING);
            }
            return;
        }

        golem.getNavigation().stop();
        if (--this.windupTicks <= 0) {
            this.throwHealingPotion();
            golem.setAttackAnimTicks(10);
            if (golem.getPotionAction() == AlchemistGolemEntity.POTION_THROWING_HEALING) {
                golem.setPotionAction(AlchemistGolemEntity.POTION_NONE);
            }
            this.cooldown = golem.isCharged() ? CHARGED_COOLDOWN : NORMAL_COOLDOWN;
            this.windupTicks = -1;
            this.target = null;
        }
    }

    private void throwHealingPotion() {
        if (!(golem.level() instanceof ServerLevel serverLevel) || this.target == null) {
            return;
        }

        Vec3 vel = this.target.getDeltaMovement();
        double xd = this.target.getX() + vel.x - golem.getX();
        double yd = this.target.getEyeY() - 1.1F - golem.getY();
        double zd = this.target.getZ() + vel.z - golem.getZ();
        double dist = Math.sqrt(xd * xd + zd * zd);

        ItemStack potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        Projectile.spawnProjectileUsingShoot(
                ThrownSplashPotion::new,
                serverLevel,
                potionStack,
                golem,
                xd,
                yd + dist * 0.2D,
                zd,
                dist <= 2.0D ? 0.45F : 0.75F,
                4.0F
        );
        serverLevel.playSound(
                null,
                golem.getX(),
                golem.getY(),
                golem.getZ(),
                SoundEvents.WITCH_THROW,
                golem.getSoundSource(),
                1.0F,
                0.8F + golem.getRandom().nextFloat() * 0.4F
        );
    }
}
