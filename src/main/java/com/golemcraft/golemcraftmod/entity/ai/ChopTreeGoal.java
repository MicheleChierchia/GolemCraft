package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.LumberjackGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class ChopTreeGoal extends Goal {
    private final LumberjackGolemEntity golem;
    private final double speed;
    private BlockPos targetLog;
    private int breakTime;
    private int searchCooldown;

    public ChopTreeGoal(LumberjackGolemEntity golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!golem.hasAxe()) return false;
        if (golem.isChopping()) return true;
        if (golem.getChestPos() == BlockPos.ZERO) {
            BlockPos currentPos = this.golem.blockPosition();
            java.util.Optional<BlockPos> nearestChest = BlockPos.betweenClosedStream(
                    currentPos.offset(-10, -3, -10),
                    currentPos.offset(10, 3, 10)
            ).map(BlockPos::immutable)
             .filter(pos -> this.golem.level().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)
             .min(java.util.Comparator.comparingDouble(pos -> currentPos.distSqr(pos)));
            if (nearestChest.isPresent()) {
                golem.setChestPos(nearestChest.get());
            } else {
                return false;
            }
        }
        if (isInventoryFull()) return false;

        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        this.targetLog = findNearestLog();
        if (this.targetLog != null) {
            return true;
        } else {
            searchCooldown = 40; // 2 seconds cooldown if no tree found
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!golem.hasAxe()) return false;
        if (isInventoryFull()) return false;
        return this.targetLog != null && (isLog(golem.level().getBlockState(this.targetLog)) || isLeaf(golem.level().getBlockState(this.targetLog)));
    }

    @Override
    public void start() {
        this.golem.setChopping(true);
        this.breakTime = 0;
        this.golem.getNavigation().moveTo(this.targetLog.getX(), this.targetLog.getY(), this.targetLog.getZ(), this.speed);
    }

    @Override
    public void stop() {
        this.golem.setChopping(false);
        this.targetLog = null;
        this.breakTime = 0;
    }

    @Override
    public void tick() {
        if (this.targetLog == null) return;

        this.golem.getLookControl().setLookAt(this.targetLog.getX() + 0.5, this.targetLog.getY() + 0.5, this.targetLog.getZ() + 0.5, 10.0F, this.golem.getMaxHeadXRot());

        double dx = this.targetLog.getX() + 0.5 - this.golem.getX();
        double dz = this.targetLog.getZ() + 0.5 - this.golem.getZ();

        if (dx * dx + dz * dz > 4.0D) {
            this.golem.getNavigation().moveTo(this.targetLog.getX(), this.targetLog.getY(), this.targetLog.getZ(), this.speed);
            
            // Se la navigazione si è fermata ma siamo ancora lontani (foglie che bloccano)
            if (this.golem.getNavigation().isDone() && this.breakTime == 0) {
                BlockPos blockingLeaf = findBlockingLeaf();
                if (blockingLeaf != null) {
                    this.targetLog = blockingLeaf;
                } else {
                    // Non troviamo foglie da rompere e non arriviamo all'albero: annulla
                    this.targetLog = null;
                }
            }
        } else {
            this.golem.getNavigation().stop();
            this.breakTime++;
            
            int animInterval = this.golem.isCharged() ? 5 : 15;
            // Sync attack animation visually
            if (this.breakTime % animInterval == 0) {
                this.golem.setAttackAnimTicks(this.golem.isCharged() ? 5 : 10);
            }
            
            int maxBreak = this.golem.isCharged() ? 12 : 40;
            if (this.breakTime >= maxBreak) { // 0.6s if charged, 2s normally
                chopOneLog();
                this.breakTime = 0;
                
                // Collect dropped items nearby
                collectDrops();
                
                // (Sapling planting logic moved to PlantSaplingGoal)
                
                // Trova il prossimo blocco connesso (per gestire rami e alberi giganti 2x2)
                BlockPos nextLog = findConnectedLog(this.targetLog);
                this.targetLog = nextLog;
                if (this.targetLog == null) {
                    // Cerca un altro tronco dal basso (nel caso di alberi della giungla molto spessi)
                    this.targetLog = findNearestLog();
                }
            }
        }
    }

    private void chopOneLog() {
        if (!(this.golem.level() instanceof ServerLevel serverLevel)) return;
        
        BlockPos pos = this.targetLog;
        if (isLog(serverLevel.getBlockState(pos)) || isLeaf(serverLevel.getBlockState(pos))) {
            serverLevel.destroyBlock(pos, true);
            
            // Damage the axe
            ItemStack axe = this.golem.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (!axe.isEmpty() && axe.isDamageableItem()) {
                axe.hurtAndBreak(1, serverLevel, null, item -> {
                    this.golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    this.golem.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
                });
            }
        }
    }

    // tryPlantSapling removed

    private void collectDrops() {
        AABB aabb = this.golem.getBoundingBox().inflate(16.0D, 16.0D, 16.0D);
        List<ItemEntity> items = this.golem.level().getEntitiesOfClass(ItemEntity.class, aabb);
        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            ItemStack remainder = this.golem.getInventory().addItem(stack);
            if (remainder.isEmpty()) {
                itemEntity.discard();
                this.golem.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.2F, 1.5F);
                this.golem.setLastPickupTime(this.golem.level().getGameTime());
            } else {
                itemEntity.setItem(remainder);
                if (stack.getCount() != remainder.getCount()) {
                    this.golem.setLastPickupTime(this.golem.level().getGameTime());
                }
            }
        }
    }

    private BlockPos findNearestLog() {
        BlockPos chestPos = this.golem.getChestPos();
        if (chestPos == BlockPos.ZERO) return null;
        
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;
        
        // Search in a 16x16 area around the chest
        int radius = 16;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 8; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = chestPos.offset(x, y, z);
                    if (isLog(this.golem.level().getBlockState(pos))) {
                        // Ensure we find the bottom-most log
                        BlockPos bottomPos = pos;
                        while (isLog(this.golem.level().getBlockState(bottomPos.below()))) {
                            bottomPos = bottomPos.below();
                        }
                        
                        double dist = this.golem.distanceToSqr(bottomPos.getX(), bottomPos.getY(), bottomPos.getZ());
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestPos = bottomPos;
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }
    
    private boolean isLeaf(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    private BlockPos findBlockingLeaf() {
        BlockPos current = this.golem.blockPosition();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = current.offset(x, y, z);
                    if (isLeaf(this.golem.level().getBlockState(checkPos))) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findConnectedLog(BlockPos pos) {
        // Priorità: sopra, lati, sotto (per i rami della giungla)
        for (int y = 1; y >= -1; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (isLog(this.golem.level().getBlockState(checkPos))) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isInventoryFull() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            if (this.golem.getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
