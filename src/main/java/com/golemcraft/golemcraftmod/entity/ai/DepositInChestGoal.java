package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.EnumSet;
import java.util.Optional;

public class DepositInChestGoal extends Goal {
    private final BaseGolemEntity golem;
    private BlockPos targetChestPos;
    private int depositTicks;

    public DepositInChestGoal(BaseGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!hasItemsToDeposit()) {
            return false;
        }

        boolean timeElapsed = this.golem.level().getGameTime() - this.golem.getLastPickupTime() >= 100;
        if (!isInventoryFull() && !timeElapsed) {
            return false;
        }


        // Search for nearest chest within 10 blocks
        BlockPos currentPos = this.golem.blockPosition();
        Optional<BlockPos> nearestChest = BlockPos.betweenClosedStream(
                currentPos.offset(-10, -3, -10),
                currentPos.offset(10, 3, 10)
        ).map(BlockPos::immutable)
         .filter(pos -> this.golem.level().getBlockEntity(pos) instanceof ChestBlockEntity)
         .filter(pos -> this.golem.getNavigation().createPath(pos, 1) != null || this.golem.getNavigation().createPath(pos.above(), 1) != null)
         .min(java.util.Comparator.comparingDouble(pos -> currentPos.distSqr(pos)));

        if (nearestChest.isPresent()) {
            this.targetChestPos = nearestChest.get();
            return true;
        }

        return false;
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

                if (this.depositTicks == 0) {
                    this.golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    BlockEntity blockEntity = this.golem.level().getBlockEntity(this.targetChestPos);
                    if (blockEntity instanceof Container container) {
                        container.startOpen(this.golem);
                    }
                    this.golem.setRummaging(true);
                }

                this.depositTicks++;

                if (this.depositTicks >= 15) {
                    // Deposit item
                    BlockEntity blockEntity = this.golem.level().getBlockEntity(this.targetChestPos);
                    if (blockEntity instanceof Container container) {
                        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
                            ItemStack stack = this.golem.getInventory().getItem(i);
                            if (!stack.isEmpty()) {
                                if (this.golem instanceof com.golemcraft.golemcraftmod.entity.FarmerGolemEntity && stack.getItem() instanceof net.minecraft.world.item.HoeItem) {
                                    continue; // Keep hoes!
                                }
                                ItemStack remainder = insertItem(container, stack.copy());
                                this.golem.getInventory().setItem(i, remainder);
                            }
                        }
                    }
                    if (blockEntity instanceof Container container) {
                        container.stopOpen(this.golem);
                    }
                    this.golem.setRummaging(false);
                    this.targetChestPos = null; // Done
                    if (!(this.golem instanceof com.golemcraft.golemcraftmod.entity.FarmerGolemEntity)) {
                        this.golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, this.golem.getInventory().getItem(0).copy()); // Update visual item
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
    
    private void moveToChest() {
        net.minecraft.world.level.pathfinder.Path path = this.golem.getNavigation().createPath(this.targetChestPos, 1);
        if (path == null) {
            path = this.golem.getNavigation().createPath(this.targetChestPos.above(), 1);
        }
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
            if (!stack.isEmpty()) {
                if (this.golem instanceof com.golemcraft.golemcraftmod.entity.FarmerGolemEntity && stack.getItem() instanceof net.minecraft.world.item.HoeItem) {
                    continue;
                }
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
        if (this.targetChestPos != null && this.depositTicks > 0 && this.depositTicks < 15) {
            // Se interrotto mentre la cesta era aperta, la chiudiamo
            BlockEntity blockEntity = this.golem.level().getBlockEntity(this.targetChestPos);
            if (blockEntity instanceof Container container) {
                container.stopOpen(this.golem);
            }
            this.golem.setRummaging(false);
        }
        this.targetChestPos = null;
        this.depositTicks = 0;
        this.golem.getNavigation().stop();
    }
}
