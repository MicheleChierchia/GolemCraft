package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.FarmerGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.EnumSet;

public class FarmerTillGoal extends Goal {
    private final FarmerGolemEntity golem;
    private BlockPos targetPos;
    private int tillTicks;

    private int scanCooldown = 0;

    public FarmerTillGoal(FarmerGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }
        ItemStack mainHand = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof HoeItem)) {
            return false;
        }

        Level level = this.golem.level();
        BlockPos currentPos = this.golem.blockPosition();

        java.util.List<BlockPos> validBlocks = new java.util.ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-8, -2, -8), currentPos.offset(8, 2, 8))) {
            BlockState state = level.getBlockState(pos);
            if ((state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH)) && level.getBlockState(pos.above()).isAir()) {
                validBlocks.add(pos.immutable());
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

    @Override
    public void start() {
        this.tillTicks = 0;
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
            this.golem.getLookControl().setLookAt(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D);
            this.tillTicks++;
            
            if (this.tillTicks >= 15) { // 0.75 seconds to till
                Level level = this.golem.level();
                BlockState state = level.getBlockState(this.targetPos);
                
                if ((state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH)) && level.getBlockState(this.targetPos.above()).isAir()) {
                    level.setBlock(this.targetPos, Blocks.FARMLAND.defaultBlockState(), 11);
                    level.playSound(null, this.targetPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, this.targetPos, GameEvent.Context.of(this.golem, Blocks.FARMLAND.defaultBlockState()));
                    
                    ItemStack hoe = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
                    if (hoe.isDamageableItem() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        hoe.hurtAndBreak(1, serverLevel, null, (item) -> {
                            this.golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            this.golem.playSound(SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
                        });
                    }
                }
                
                this.targetPos = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPos == null) return false;
        ItemStack hoe = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
        if (hoe.isEmpty() || !(hoe.getItem() instanceof HoeItem)) {
            return false;
        }
        if (this.golem.getNavigation().isDone() && this.golem.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 1, this.targetPos.getZ() + 0.5D) > 4.0D) {
            return false;
        }
        BlockState state = this.golem.level().getBlockState(this.targetPos);
        return (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH)) && this.golem.level().getBlockState(this.targetPos.above()).isAir();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.tillTicks = 0;
        this.golem.getNavigation().stop();
    }
}
