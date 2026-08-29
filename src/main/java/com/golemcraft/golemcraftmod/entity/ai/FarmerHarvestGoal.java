package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.FarmerGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumSet;

public class FarmerHarvestGoal extends Goal {
    private final FarmerGolemEntity golem;
    private BlockPos targetPos;
    private int breakTicks;

    private int scanCooldown = 0;

    public FarmerHarvestGoal(FarmerGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }
        Level level = this.golem.level();
        BlockPos currentPos = this.golem.blockPosition();

        java.util.List<BlockPos> validCrops = new java.util.ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-8, -2, -8), currentPos.offset(8, 2, 8))) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
                validCrops.add(pos.immutable());
            } else if (block == net.minecraft.world.level.block.Blocks.MELON || block == net.minecraft.world.level.block.Blocks.PUMPKIN) {
                validCrops.add(pos.immutable());
            }
        }

        if (validCrops.isEmpty()) return false;

        validCrops.sort(java.util.Comparator.comparingDouble(currentPos::distSqr));

        for (BlockPos pos : validCrops) {
            net.minecraft.world.level.pathfinder.Path path = this.golem.getNavigation().createPath(pos, 1);
            if (path != null && path.canReach()) {
                this.targetPos = pos;
                return true;
            }
        }

        this.scanCooldown = 10;
        return false;
    }

    @Override
    public void start() {
        this.breakTicks = 0;
        double speed = this.golem.isCharged() ? 1.4D : 1.2D;
        this.golem.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, speed);
    }

    @Override
    public void tick() {
        if (this.targetPos == null) return;

        double dist = this.golem.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D);
        
        if (dist > 4.0D) {
            double speed = this.golem.isCharged() ? 1.4D : 1.2D;
            this.golem.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, speed);
        } else {
            this.golem.getNavigation().stop();
            this.golem.getLookControl().setLookAt(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D);
            this.breakTicks++;
            
            int maxBreak = this.golem.isCharged() ? 4 : 15;
            if (this.breakTicks >= maxBreak) {
                Level level = this.golem.level();
                BlockState state = level.getBlockState(this.targetPos);
                
                Block block = state.getBlock();
                boolean isCrop = block instanceof CropBlock crop && crop.isMaxAge(state);
                boolean isFruit = block == net.minecraft.world.level.block.Blocks.MELON || block == net.minecraft.world.level.block.Blocks.PUMPKIN;
                
                if ((isCrop || isFruit) && level instanceof ServerLevel serverLevel) {
                    java.util.List<ItemStack> drops = Block.getDrops(state, serverLevel, this.targetPos, null, this.golem, this.golem.getMainHandItem());
                    
                    if (isCrop) {
                        CropBlock crop = (CropBlock) block;
                        boolean seedFound = false;
                        
                        // First try to replant from drops
                        for (ItemStack drop : drops) {
                            if (!seedFound && drop.getItem() instanceof BlockItem bi && bi.getBlock() == crop) {
                                seedFound = true;
                                drop.shrink(1); // Consume one for replanting
                            }
                            
                            // Add remainder to inventory or drop in world
                            if (!drop.isEmpty()) {
                                ItemStack remainder = this.golem.getInventory().addItem(drop);
                                this.golem.setLastPickupTime(level.getGameTime());
                                if (!remainder.isEmpty()) {
                                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5, remainder);
                                    level.addFreshEntity(itemEntity);
                                }
                            }
                        }
                        
                        // If no seed in drops, check inventory
                        if (!seedFound) {
                            for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
                                ItemStack invStack = this.golem.getInventory().getItem(i);
                                if (invStack.getItem() instanceof BlockItem bi && bi.getBlock() == crop) {
                                    seedFound = true;
                                    invStack.shrink(1);
                                    break;
                                }
                            }
                        }
                        
                        if (seedFound) {
                            level.setBlock(this.targetPos, crop.defaultBlockState(), 3); // Replant
                        } else {
                            level.destroyBlock(this.targetPos, false); // Just break if no seed
                        }
                    } else if (isFruit) {
                        for (ItemStack drop : drops) {
                            if (!drop.isEmpty()) {
                                ItemStack remainder = this.golem.getInventory().addItem(drop);
                                this.golem.setLastPickupTime(level.getGameTime());
                                if (!remainder.isEmpty()) {
                                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5, remainder);
                                    level.addFreshEntity(itemEntity);
                                }
                            }
                        }
                        level.destroyBlock(this.targetPos, false);
                    }
                }
                
                this.targetPos = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPos == null) return false;
        if (this.golem.getNavigation().isDone() && this.golem.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D) > 4.0D) {
            return false;
        }
        BlockState state = this.golem.level().getBlockState(this.targetPos);
        Block block = state.getBlock();
        return (block instanceof CropBlock crop && crop.isMaxAge(state)) || block == net.minecraft.world.level.block.Blocks.MELON || block == net.minecraft.world.level.block.Blocks.PUMPKIN;
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.breakTicks = 0;
        this.golem.actionCooldown = this.golem.isCharged() ? 3 : 15;
        this.golem.getNavigation().stop();
    }
}
