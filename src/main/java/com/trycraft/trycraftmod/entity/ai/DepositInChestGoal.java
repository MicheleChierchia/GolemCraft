package com.trycraft.trycraftmod.entity.ai;

import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.EnumSet;
import java.util.Optional;

public class DepositInChestGoal extends Goal {
    private final FlowerGolemEntity golem;
    private BlockPos targetChestPos;
    private int depositTicks;

    public DepositInChestGoal(FlowerGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.golem.getMainHandItem().isEmpty()) {
            return false; // No flowers to deposit
        }

        // Search for nearest chest within 10 blocks
        BlockPos currentPos = this.golem.blockPosition();
        Optional<BlockPos> nearestChest = BlockPos.betweenClosedStream(
                currentPos.offset(-10, -3, -10),
                currentPos.offset(10, 3, 10)
        ).filter(pos -> this.golem.level().getBlockEntity(pos) instanceof ChestBlockEntity)
         .findFirst().map(BlockPos::immutable);

        if (nearestChest.isPresent()) {
            this.targetChestPos = nearestChest.get();
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        this.depositTicks = 0;
        this.golem.getNavigation().moveTo(this.targetChestPos.getX() + 0.5, this.targetChestPos.getY(), this.targetChestPos.getZ() + 0.5, 1.2D);
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
                        ItemStack flower = this.golem.getMainHandItem();
                        if (!flower.isEmpty()) {
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                if (container.getItem(i).isEmpty() || (ItemStack.isSameItemSameComponents(container.getItem(i), flower) && container.getItem(i).getCount() < container.getItem(i).getMaxStackSize())) {
                                    ItemStack remainder = insertItem(container, flower.copy());
                                    this.golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, remainder);
                                    break;
                                }
                            }
                        }
                    }
                    if (blockEntity instanceof Container container) {
                        container.stopOpen(this.golem);
                    }
                    this.golem.setRummaging(false);
                    this.targetChestPos = null; // Done
                }
            } else {
                // Keep moving
                this.golem.getNavigation().moveTo(this.targetChestPos.getX() + 0.5, this.targetChestPos.getY(), this.targetChestPos.getZ() + 0.5, 1.2D);
            }
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

    @Override
    public boolean canContinueToUse() {
        return this.targetChestPos != null && !this.golem.getMainHandItem().isEmpty();
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
