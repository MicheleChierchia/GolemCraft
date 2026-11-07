package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BeekeeperGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class BeekeeperHarvestGoal extends Goal {
    private final BeekeeperGolemEntity golem;
    private BlockPos targetHivePos;
    private int checkCooldown = 0;
    private int harvestTicks = 0;

    public BeekeeperHarvestGoal(BeekeeperGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.checkCooldown-- > 0) return false;
        this.checkCooldown = 20;

        BlockPos currentPos = this.golem.blockPosition();
        Level level = this.golem.level();
        double closestDistSq = Double.MAX_VALUE;
        BlockPos foundPos = null;

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-16, -4, -16), currentPos.offset(16, 4, 16))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.BEEHIVE) && state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                double distSq = pos.distSqr(currentPos);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    foundPos = pos.immutable();
                }
            }
        }

        if (foundPos != null) {
            this.targetHivePos = foundPos;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetHivePos == null) return false;
        BlockState state = this.golem.level().getBlockState(this.targetHivePos);
        return state.is(Blocks.BEEHIVE) && state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getValue(BeehiveBlock.HONEY_LEVEL) >= 5 && this.harvestTicks < 100;
    }

    @Override
    public void start() {
        this.harvestTicks = 0;
        if (this.targetHivePos != null) {
            this.golem.getNavigation().moveTo(this.targetHivePos.getX() + 0.5, this.targetHivePos.getY(), this.targetHivePos.getZ() + 0.5, 1.15D);
        }
    }

    @Override
    public void tick() {
        if (this.targetHivePos == null) return;
        this.harvestTicks++;
        this.golem.getLookControl().setLookAt(this.targetHivePos.getX() + 0.5, this.targetHivePos.getY() + 0.5, this.targetHivePos.getZ() + 0.5);

        double distSq = this.golem.distanceToSqr(this.targetHivePos.getX() + 0.5, this.targetHivePos.getY() + 0.5, this.targetHivePos.getZ() + 0.5);
        if (distSq <= 4.0D) {
            // Reached hive - harvest honeycombs peacefully without angering bees
            Level level = this.golem.level();
            BlockState state = level.getBlockState(this.targetHivePos);
            if (state.is(Blocks.BEEHIVE) && state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                // Reset hive honey level safely
                level.setBlock(this.targetHivePos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0), 3);
                level.playSound(null, this.targetHivePos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LANDING_HONEY, this.targetHivePos.getX() + 0.5D, this.targetHivePos.getY() + 0.5D, this.targetHivePos.getZ() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.05D);
                }

                // Add 3 honeycombs to golem inventory
                ItemStack honeycombs = new ItemStack(Items.HONEYCOMB, 3);
                ItemStack remainder = this.golem.getInventory().addItem(honeycombs);
                if (!remainder.isEmpty()) {
                    this.golem.spawnAtLocation(serverLevel(level), remainder);
                }

                // Increase honey accumulation on golem (up to 5)
                this.golem.incrementHoneyLevel();
                this.golem.setLastPickupTime(level.getGameTime());
                this.golem.actionCooldown = 30;
            }
            this.targetHivePos = null;
        } else if (this.golem.getNavigation().isDone()) {
            this.golem.getNavigation().moveTo(this.targetHivePos.getX() + 0.5, this.targetHivePos.getY(), this.targetHivePos.getZ() + 0.5, 1.15D);
        }
    }

    private ServerLevel serverLevel(Level level) {
        return level instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    @Override
    public void stop() {
        this.targetHivePos = null;
        this.harvestTicks = 0;
    }
}
