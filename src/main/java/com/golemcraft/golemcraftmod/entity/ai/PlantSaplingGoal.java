package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.LumberjackGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;
import java.util.Optional;

public class PlantSaplingGoal extends Goal {
    private final LumberjackGolemEntity golem;
    private final double speed;
    private BlockPos targetPos;
    private int plantTicks;

    public PlantSaplingGoal(LumberjackGolemEntity golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (golem.isChopping()) return false;

        // Check if we have a sapling
        if (getSaplingIndex() == -1) return false;

        // Find a dirt/grass block with air above it
        BlockPos currentPos = this.golem.blockPosition();
        Optional<BlockPos> nearestPlantSpot = BlockPos.betweenClosedStream(
                currentPos.offset(-8, -2, -8),
                currentPos.offset(8, 2, 8)
        ).map(BlockPos::immutable)
         .filter(pos -> {
             net.minecraft.world.level.block.state.BlockState below = golem.level().getBlockState(pos.below());
             net.minecraft.world.level.block.state.BlockState current = golem.level().getBlockState(pos);
             return (below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK)) && current.canBeReplaced();
         })
         .filter(pos -> this.golem.getNavigation().createPath(pos, 1) != null)
         .min(java.util.Comparator.comparingDouble(pos -> currentPos.distSqr(pos)));

        if (nearestPlantSpot.isPresent()) {
            this.targetPos = nearestPlantSpot.get();
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        this.plantTicks = 0;
        this.golem.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
    }

    @Override
    public void tick() {
        if (this.targetPos != null) {
            if (this.golem.distanceToSqr(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5) < 4.0D) {
                this.golem.getNavigation().stop();
                this.golem.getLookControl().setLookAt(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 0.5D, this.targetPos.getZ() + 0.5D);

                if (this.plantTicks == 0) {
                    this.golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }

                this.plantTicks++;

                if (this.plantTicks >= 15) {
                    // Plant sapling
                    int saplingIndex = getSaplingIndex();
                    if (saplingIndex != -1) {
                        ItemStack saplingStack = this.golem.getInventory().getItem(saplingIndex);
                        if (saplingStack.getItem() instanceof BlockItem blockItem) {
                            Block saplingBlock = blockItem.getBlock();
                            net.minecraft.world.level.block.state.BlockState below = golem.level().getBlockState(targetPos.below());
                            net.minecraft.world.level.block.state.BlockState current = golem.level().getBlockState(targetPos);
                            
                            if ((below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK)) && current.canBeReplaced()) {
                                golem.level().setBlock(targetPos, saplingBlock.defaultBlockState(), 3);
                                saplingStack.shrink(1);
                                this.golem.playSound(net.minecraft.sounds.SoundEvents.GRASS_PLACE, 1.0F, 1.0F);
                            }
                        }
                    }
                    this.targetPos = null; // Done
                }
            } else {
                if (this.golem.getNavigation().isDone()) {
                    this.golem.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPos != null && getSaplingIndex() != -1;
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.plantTicks = 0;
        this.golem.getNavigation().stop();
    }

    private int getSaplingIndex() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.SAPLINGS)) {
                return i;
            }
        }
        return -1;
    }
}
