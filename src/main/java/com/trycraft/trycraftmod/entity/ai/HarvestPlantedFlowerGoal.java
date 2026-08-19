package com.trycraft.trycraftmod.entity.ai;

import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class HarvestPlantedFlowerGoal extends Goal {
    private final FlowerGolemEntity golem;
    private BlockPos targetFlowerPos;
    private int breakTicks;

    public HarvestPlantedFlowerGoal(FlowerGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.golem.getMainHandItem().isEmpty()) {
            return false;
        }

        Level level = this.golem.level();
        BlockPos currentPos = this.golem.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-8, -2, -8), currentPos.offset(8, 2, 8))) {
            BlockState state = level.getBlockState(pos);
            if (isFlowerBlock(state.getBlock())) {
                this.targetFlowerPos = pos.immutable();
                return true;
            }
        }
        return false;
    }

    private boolean isFlowerBlock(Block block) {
        return block instanceof FlowerBlock || block instanceof TallFlowerBlock;
    }

    @Override
    public void start() {
        this.breakTicks = 0;
        this.golem.getNavigation().moveTo(this.targetFlowerPos.getX() + 0.5D, this.targetFlowerPos.getY(), this.targetFlowerPos.getZ() + 0.5D, 1.2D);
    }

    @Override
    public void tick() {
        if (this.targetFlowerPos == null) return;

        double dist = this.golem.distanceToSqr(this.targetFlowerPos.getX() + 0.5D, this.targetFlowerPos.getY(), this.targetFlowerPos.getZ() + 0.5D);
        
        if (dist > 4.0D) {
            this.golem.getNavigation().moveTo(this.targetFlowerPos.getX() + 0.5D, this.targetFlowerPos.getY(), this.targetFlowerPos.getZ() + 0.5D, 1.2D);
        } else {
            this.golem.getNavigation().stop();
            this.golem.getLookControl().setLookAt(this.targetFlowerPos.getX() + 0.5D, this.targetFlowerPos.getY(), this.targetFlowerPos.getZ() + 0.5D);
            this.breakTicks++;
            
            if (this.breakTicks >= 40) { // 2 seconds to break
                Level level = this.golem.level();
                BlockState state = level.getBlockState(this.targetFlowerPos);
                
                if (isFlowerBlock(state.getBlock())) {
                    // Harvest it directly into hand
                    ItemStack drop = new ItemStack(state.getBlock().asItem());
                    this.golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, drop);
                    level.destroyBlock(this.targetFlowerPos, false);
                }
                
                this.targetFlowerPos = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetFlowerPos != null && this.golem.getMainHandItem().isEmpty();
    }

    @Override
    public void stop() {
        this.targetFlowerPos = null;
        this.breakTicks = 0;
        this.golem.getNavigation().stop();
    }
}
