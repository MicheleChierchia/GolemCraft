package com.golemcraft.golemcraftmod.block;

import com.golemcraft.golemcraftmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

public class GolemBeaconMenu extends AbstractContainerMenu {

    private final SimpleContainerData syncedData = new SimpleContainerData(5);
    private final net.minecraft.world.inventory.ContainerData beaconData;
    private final BlockPos bePos;
    private final Container paymentSlot;

    @Nullable
    private final GolemBeaconBlockEntity blockEntity;

    class BeaconPaymentSlot extends Slot {
        public BeaconPaymentSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(net.minecraft.world.item.Items.COPPER_INGOT);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    public GolemBeaconMenu(int windowId, Inventory inv, GolemBeaconBlockEntity be) {
        super(ModBlockEntities.GOLEM_BEACON_MENU.get(), windowId);
        this.bePos = be.getBlockPos();
        this.blockEntity = be;
        this.paymentSlot = be.getPaymentSlot();
        this.beaconData = be.containerData;
        this.addDataSlots(this.beaconData);
        this.setupSlots(inv);
    }

    public GolemBeaconMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(ModBlockEntities.GOLEM_BEACON_MENU.get(), windowId);
        this.bePos = data.readBlockPos();
        this.blockEntity = null;
        this.paymentSlot = new SimpleContainer(1);
        int t1 = data.readInt();
        int t2 = data.readInt();
        int t3 = data.readInt();
        int sec = data.readInt();
        int lvl = data.readInt();
        this.syncedData.set(0, t1);
        this.syncedData.set(1, t2);
        this.syncedData.set(2, t3);
        this.syncedData.set(3, sec);
        this.syncedData.set(4, lvl);
        this.beaconData = this.syncedData;
        this.addDataSlots(this.beaconData);
        this.setupSlots(inv);
    }
    
    private void setupSlots(Inventory inv) {
        this.addSlot(new BeaconPaymentSlot(this.paymentSlot, 0, 136, 110));

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 36 + j * 18, 137 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inv, i, 36 + i * 18, 195));
        }
    }

    public int getTier1Effect() { return beaconData.get(0); }
    public int getTier2Effect() { return beaconData.get(1); }
    public int getTier3Effect() { return beaconData.get(2); }
    public int getSecondaryEffect() { return beaconData.get(3); }
    public int getLevels() { return beaconData.get(4); }
    public BlockPos getBeaconPos() { return bePos; }
    public boolean hasPayment() { return !this.paymentSlot.getItem(0).isEmpty(); }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                .evaluate((level, pos) ->
                        level.getBlockEntity(pos) == blockEntity
                                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0,
                        true);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.paymentSlot.hasAnyMatching(ItemStack::isEmpty) && this.paymentSlot.canPlaceItem(0, itemstack1) && itemstack1.getCount() == 1) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
