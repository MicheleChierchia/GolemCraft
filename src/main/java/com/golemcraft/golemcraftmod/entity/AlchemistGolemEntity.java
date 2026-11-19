package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.entity.ai.AlchemistHealAllyGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Companion golem that heals nearby allies with splash potions and drinks invisibility when hit.
 */
public class AlchemistGolemEntity extends CompanionGolemEntity {

    public static final int POTION_NONE = 0;
    public static final int POTION_DRINKING_INVISIBILITY = 1;
    public static final int POTION_THROWING_HEALING = 2;

    private static final int DRINK_DURATION_TICKS = 28;
    private static final int INVISIBILITY_DURATION_TICKS = 200;
    private static final int INVISIBILITY_COOLDOWN_TICKS = 400;

    private static final EntityDataAccessor<Integer> POTION_ACTION =
            SynchedEntityData.defineId(AlchemistGolemEntity.class, EntityDataSerializers.INT);

    private int drinkingTicks = 0;
    private int invisibilityCooldown = 0;

    public AlchemistGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POTION_ACTION, POTION_NONE);
    }

    public int getPotionAction() {
        return this.entityData.get(POTION_ACTION);
    }

    public void setPotionAction(int action) {
        this.entityData.set(POTION_ACTION, action);
    }

    public boolean isDrinkingPotion() {
        return this.getPotionAction() == POTION_DRINKING_INVISIBILITY;
    }

    @Override
    public boolean shouldSyncSlot0ToMainHand() {
        // Potion rendering uses POTION_ACTION rather than MAIN_HAND to avoid free potion drops on death/reset.
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new AlchemistHealAllyGoal(this));
        this.registerCompanionGoals(3, 4, 5);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        boolean hurt = super.hurtServer(level, damageSource, amount);
        if (hurt && this.isAlive() && this.getOxidationLevel() < 3
                && !this.isDrinkingPotion()
                && this.invisibilityCooldown <= 0
                && !this.hasEffect(MobEffects.INVISIBILITY)) {
            this.startDrinkingInvisibility();
        }
        return hurt;
    }

    private void startDrinkingInvisibility() {
        this.drinkingTicks = DRINK_DURATION_TICKS;
        this.setPotionAction(POTION_DRINKING_INVISIBILITY);
        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.WITCH_DRINK,
                this.getSoundSource(),
                1.0F,
                0.8F + this.random.nextFloat() * 0.4F
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        if (this.invisibilityCooldown > 0) {
            this.invisibilityCooldown--;
        }

        if (this.drinkingTicks > 0) {
            if (this.getOxidationLevel() == 3) {
                this.drinkingTicks = 0;
                this.setPotionAction(POTION_NONE);
                return;
            }

            this.drinkingTicks--;
            if (this.drinkingTicks == 0) {
                this.setPotionAction(POTION_NONE);
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION_TICKS, 0));
                this.invisibilityCooldown = INVISIBILITY_COOLDOWN_TICKS;
                this.gameEvent(GameEvent.DRINK);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.7D, this.getZ(), 12, 0.25D, 0.3D, 0.25D, 0.02D);
                    for (Mob mob : sl.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(24.0D), m -> m.getTarget() == this)) {
                        mob.setTarget(null);
                    }
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("InvisibilityCooldown", this.invisibilityCooldown);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.invisibilityCooldown = input.getIntOr("InvisibilityCooldown", 0);
    }
}
