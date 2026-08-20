package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.FarmerGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.EnumSet;

public class FarmerPlantGoal extends Goal {
    private final FarmerGolemEntity golem;
    private BlockPos targetPos;
    private int plantTicks;

    private int scanCooldown = 0;

    public FarmerPlantGoal(FarmerGolemEntity golem) {
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
        if (!hasSeeds()) return false;

        Level level = this.golem.level();
        BlockPos currentPos = this.golem.blockPosition();

        java.util.List<BlockPos> validBlocks = new java.util.ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-8, -2, -8), currentPos.offset(8, 2, 8))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir()) {
                if (!isReservedForFruit(level, pos)) {
                    validBlocks.add(pos.immutable());
                }
            }
        }
        
        if (validBlocks.isEmpty()) return false;
        
        validBlocks.sort(java.util.Comparator.comparingDouble(currentPos::distSqr));
        
        for (BlockPos pos : validBlocks) {
            net.minecraft.world.level.pathfinder.Path path = this.golem.getNavigation().createPath(pos, 1);
            if (path != null && path.canReach()) {
                this.targetPos = pos;
                return true;
            }
        }

        this.scanCooldown = 10;
        return false;
    }

    private boolean hasSeeds() {
        return !getSeedStack().isEmpty();
    }

    private ItemStack getSeedStack() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof net.minecraft.world.level.block.CropBlock || block instanceof net.minecraft.world.level.block.StemBlock) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void start() {
        this.plantTicks = 0;
        this.golem.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, this.targetPos.getZ() + 0.5D, 1.2D);
    }

    @Override
    public void tick() {
        if (this.targetPos == null) return;

        double dist = this.golem.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, this.targetPos.getZ() + 0.5D);
        
        if (dist > 4.0D) {
            this.golem.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, this.targetPos.getZ() + 0.5D, 1.2D);
        } else {
            this.golem.getNavigation().stop();
            this.golem.getLookControl().setLookAt(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, this.targetPos.getZ() + 0.5D);
            if (this.plantTicks == 0) {
                this.golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            this.plantTicks++;
            
            if (this.plantTicks >= 15) { // 0.75 seconds to plant
                Level level = this.golem.level();
                BlockState state = level.getBlockState(this.targetPos);
                
                if (state.is(Blocks.FARMLAND) && level.getBlockState(this.targetPos.above()).isAir()) {
                    ItemStack seedStack = getSeedStack();
                    if (!seedStack.isEmpty()) {
                        Block plantBlock = null;
                        if (seedStack.getItem() instanceof BlockItem blockItem) {
                            plantBlock = blockItem.getBlock();
                        }

                        if (plantBlock != null) {
                            level.setBlock(this.targetPos.above(), plantBlock.defaultBlockState(), 3);
                            level.playSound(null, this.targetPos.above(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                            seedStack.shrink(1);
                        }
                    }
                }
                
                this.targetPos = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPos == null) return false;
        if (!hasSeeds()) return false;
        if (this.golem.getNavigation().isDone() && this.golem.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, this.targetPos.getZ() + 0.5D) > 4.0D) {
            return false;
        }
        return this.golem.level().getBlockState(this.targetPos).is(Blocks.FARMLAND) && this.golem.level().getBlockState(this.targetPos.above()).isAir();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.plantTicks = 0;
        this.golem.actionCooldown = 15;
        this.golem.getNavigation().stop();
    }
    
    private boolean isReservedForFruit(Level level, BlockPos farmlandPos) {
        BlockPos[] neighbors = new BlockPos[]{
            farmlandPos.north().above(), farmlandPos.south().above(),
            farmlandPos.east().above(), farmlandPos.west().above()
        };
        
        for (BlockPos n : neighbors) {
            BlockState state = level.getBlockState(n);
            if (state.getBlock() instanceof net.minecraft.world.level.block.StemBlock) {
                boolean hasFruit = false;
                BlockPos[] stemNeighbors = new BlockPos[]{
                    n.north(), n.south(), n.east(), n.west()
                };
                for (BlockPos sn : stemNeighbors) {
                    Block snBlock = level.getBlockState(sn).getBlock();
                    if (snBlock == Blocks.MELON || snBlock == Blocks.PUMPKIN) {
                        hasFruit = true;
                        break;
                    }
                }
                if (!hasFruit) {
                    return true;
                }
            }
        }
        return false;
    }
}
