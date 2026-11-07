package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class DepositInChestGoal extends Goal {
    private final BaseGolemEntity golem;
    private BlockPos targetChestPos;
    private int depositTicks;
    private int searchCooldown = 0;

    public DepositInChestGoal(BaseGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }
        if (this.golem instanceof com.golemcraft.golemcraftmod.entity.LumberjackGolemEntity lumberjack && lumberjack.isChopping()) {
            return false;
        }

        if (!hasItemsToDeposit()) {
            return false;
        }

        boolean timeElapsed = this.golem.level().getGameTime() - this.golem.getLastPickupTime() >= 100;
        if (!isInventoryFull() && !timeElapsed) {
            return false;
        }

        // Check if existing target is still valid and reachable
        if (this.targetChestPos != null && isValidChest(this.targetChestPos)) {
            net.minecraft.world.level.pathfinder.Path path = getPathTo(this.targetChestPos);
            if (path != null && path.canReach()) {
                return true;
            }
        }

        // Search for nearest container within 10 blocks without allocating a sorted list
        BlockPos currentPos = this.golem.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-10, -3, -10), currentPos.offset(10, 3, 10))) {
            if (isValidChest(pos)) {
                double distSq = currentPos.distSqr(pos);
                if (distSq < bestDistSq) {
                    net.minecraft.world.level.pathfinder.Path path = getPathTo(pos);
                    if (path != null && path.canReach()) {
                        bestDistSq = distSq;
                        bestPos = pos.immutable();
                        if (distSq <= 4.0D) {
                            break; // Immediate proximity found, stop searching early
                        }
                    }
                }
            }
        }

        if (bestPos != null) {
            this.targetChestPos = bestPos;
            return true;
        }

        this.targetChestPos = null;
        this.searchCooldown = 40;
        return false;
    }

    private boolean isValidChest(BlockPos pos) {
        BlockEntity be = this.golem.level().getBlockEntity(pos);
        return be instanceof Container;
    }

    private net.minecraft.world.level.pathfinder.Path getPathTo(BlockPos pos) {
        net.minecraft.world.level.pathfinder.Path path = this.golem.getNavigation().createPath(pos, 1);
        if (path == null) {
            path = this.golem.getNavigation().createPath(pos.above(), 1);
        }
        return path;
    }

    @Override
    public void start() {
        this.depositTicks = 0;
        moveToChest();
    }

    @Override
    public void tick() {
        if (this.targetChestPos != null) {
            if (this.golem.distanceToSqr(this.targetChestPos.getX() + 0.5, this.targetChestPos.getY(), this.targetChestPos.getZ() + 0.5) < 6.0D) {
                this.golem.getNavigation().stop();
                this.golem.getLookControl().setLookAt(this.targetChestPos.getX() + 0.5D, this.targetChestPos.getY() + 0.5D, this.targetChestPos.getZ() + 0.5D);

                Container container = getContainerAt(this.targetChestPos);

                if (this.depositTicks == 0) {
                    this.golem.setAttackAnimTicks(10);
                    if (container != null) {
                        container.startOpen(this.golem);
                    }
                    this.golem.setRummaging(true);
                }

                this.depositTicks++;

                if (this.depositTicks >= 30) {
                    // Deposit items
                    if (container != null) {
                        boolean modified = false;
                        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
                            ItemStack stack = this.golem.getInventory().getItem(i);
                            if (!stack.isEmpty() && !this.golem.shouldKeepItem(stack, i)) {
                                ItemStack remainder = insertItem(container, stack.copy());
                                this.golem.getInventory().setItem(i, remainder);
                                modified = true;
                            }
                        }
                        if (modified) {
                            container.setChanged();
                        }
                        container.stopOpen(this.golem);
                    }
                    this.golem.setRummaging(false);
                    this.targetChestPos = null; // Done
                    this.golem.actionCooldown = 15;
                    if (this.golem.shouldSyncSlot0ToMainHand()) {
                        this.golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, this.golem.getInventory().getItem(0).copy());
                    }
                }
            } else {
                // Keep moving
                if (this.golem.getNavigation().isDone()) {
                    moveToChest();
                }
            }
        }
    }

    private Container getContainerAt(BlockPos pos) {
        Level level = this.golem.level();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, state, level, pos, true);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) {
            return container;
        }
        return null;
    }

    private void moveToChest() {
        if (this.targetChestPos == null) return;
        net.minecraft.world.level.pathfinder.Path path = getPathTo(this.targetChestPos);
        if (path != null) {
            this.golem.getNavigation().moveTo(path, 1.2D);
        }
    }

    private ItemStack insertItem(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space >= stack.getCount()) {
                    slotStack.grow(stack.getCount());
                    return ItemStack.EMPTY;
                } else if (space > 0) {
                    slotStack.grow(space);
                    stack.shrink(space);
                }
            }
        }
        return stack; // Return remainder if no space
    }

    private boolean hasItemsToDeposit() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (!stack.isEmpty() && !this.golem.shouldKeepItem(stack, i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            if (this.golem.getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetChestPos == null || !hasItemsToDeposit()) return false;
        if (this.golem.getNavigation().isDone() && this.golem.distanceToSqr(this.targetChestPos.getX() + 0.5, this.targetChestPos.getY(), this.targetChestPos.getZ() + 0.5) >= 6.0D) {
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        this.targetChestPos = null;
        this.depositTicks = 0;
        this.golem.setRummaging(false);
        this.golem.actionCooldown = 15;
        this.golem.getNavigation().stop();
    }
}
