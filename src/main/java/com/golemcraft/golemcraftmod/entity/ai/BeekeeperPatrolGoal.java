package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BeekeeperGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

public class BeekeeperPatrolGoal extends Goal {
    private final BeekeeperGolemEntity golem;
    private BlockPos targetHivePos;
    private int checkCooldown = 0;

    public BeekeeperPatrolGoal(BeekeeperGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.checkCooldown-- > 0) return false;
        this.checkCooldown = 40;

        // Find nearest artificial beehive within 16 blocks
        BlockPos currentPos = this.golem.blockPosition();
        Level level = this.golem.level();
        double closestDistSq = Double.MAX_VALUE;
        BlockPos foundPos = null;

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-16, -4, -16), currentPos.offset(16, 4, 16))) {
            if (level.getBlockState(pos).is(Blocks.BEEHIVE)) {
                double distSq = pos.distSqr(currentPos);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    foundPos = pos.immutable();
                }
            }
        }

        if (foundPos != null) {
            this.targetHivePos = foundPos;
            // Only move if golem is farther than 4 blocks or wandering away (> 8 blocks)
            return closestDistSq > 16.0D;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetHivePos != null && !this.golem.getNavigation().isDone() && this.golem.distanceToSqr(this.targetHivePos.getX() + 0.5, this.targetHivePos.getY(), this.targetHivePos.getZ() + 0.5) > 9.0D;
    }

    @Override
    public void start() {
        if (this.targetHivePos != null) {
            this.golem.getNavigation().moveTo(this.targetHivePos.getX() + 0.5, this.targetHivePos.getY(), this.targetHivePos.getZ() + 0.5, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.targetHivePos = null;
    }
}
