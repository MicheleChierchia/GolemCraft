package com.golemcraft.golemcraftmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class TotemGolemEntity extends CompanionGolemEntity {

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
        this.registerCompanionGoals(1, 2, 3);
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
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
            BaseGolemEntity baseGolem =
                    com.golemcraft.golemcraftmod.registry.ModEntities.BASE_GOLEM.get().create(sl, EntitySpawnReason.CONVERSION);
            if (baseGolem != null) {
                this.copyBaseDataTo(baseGolem, player);
                sl.addFreshEntity(baseGolem);
            }
        }

        this.discard();
    }
}
