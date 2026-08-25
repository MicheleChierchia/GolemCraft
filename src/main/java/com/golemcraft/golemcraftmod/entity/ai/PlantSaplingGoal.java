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
    private boolean isGiant;

    public PlantSaplingGoal(LumberjackGolemEntity golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (golem.isChopping()) return false;

        // Check if we have a sapling
        int index = getSaplingIndex();
        if (index == -1) return false;
        
        ItemStack stack = this.golem.getInventory().getItem(index);
        this.isGiant = false;
        
        // Determina se dobbiamo piantare un albero 2x2
        if (stack.is(net.minecraft.world.item.Items.DARK_OAK_SAPLING)) {
            if (countItem(net.minecraft.world.item.Items.DARK_OAK_SAPLING) < 4) return false;
            this.isGiant = true;
        } else if ((stack.is(net.minecraft.world.item.Items.JUNGLE_SAPLING) || stack.is(net.minecraft.world.item.Items.SPRUCE_SAPLING)) && countItem(stack.getItem()) >= 4) {
            this.isGiant = true;
        }

        // Find a dirt/grass block with air above it
        BlockPos currentPos = this.golem.blockPosition();
        Optional<BlockPos> nearestPlantSpot = BlockPos.betweenClosedStream(
                currentPos.offset(-8, -2, -8),
                currentPos.offset(8, 2, 8)
        ).map(BlockPos::immutable)
         .filter(pos -> {
             if (this.isGiant) {
                 // Controlla area 2x2
                 for (int dx = 0; dx <= 1; dx++) {
                     for (int dz = 0; dz <= 1; dz++) {
                         BlockPos checkPos = pos.offset(dx, 0, dz);
                         net.minecraft.world.level.block.state.BlockState below = golem.level().getBlockState(checkPos.below());
                         net.minecraft.world.level.block.state.BlockState current = golem.level().getBlockState(checkPos);
                         if (!((below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK)) && current.canBeReplaced())) {
                             return false;
                         }
                     }
                 }
                 return true;
             } else {
                 net.minecraft.world.level.block.state.BlockState below = golem.level().getBlockState(pos.below());
                 net.minecraft.world.level.block.state.BlockState current = golem.level().getBlockState(pos);
                 return (below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK)) && current.canBeReplaced();
             }
         })
         .filter(pos -> {
             // Non piazzare vicino ad altri alberi! (Raggio più ampio per i giganti)
             int radius = this.isGiant ? 3 : 2;
             for (int dx = -radius; dx <= radius + (this.isGiant ? 1 : 0); dx++) {
                 for (int dz = -radius; dz <= radius + (this.isGiant ? 1 : 0); dz++) {
                     // Ignora i blocchi che stiamo per piantare
                     if (this.isGiant && dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) continue;
                     if (!this.isGiant && dx == 0 && dz == 0) continue;
                     
                     BlockPos checkPos = pos.offset(dx, 0, dz);
                     if (golem.level().getBlockState(checkPos).is(net.minecraft.tags.BlockTags.create(net.minecraft.resources.Identifier.withDefaultNamespace("saplings"))) || 
                         golem.level().getBlockState(checkPos).is(BlockTags.LOGS) ||
                         golem.level().getBlockState(checkPos.above()).is(BlockTags.LOGS)) {
                         return false;
                     }
                 }
             }
             return true;
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
                    if (!this.golem.level().isClientSide()) {
                        this.golem.setAttackAnimTicks(10);
                    }
                }

                this.plantTicks++;

                if (this.plantTicks >= 15) {
                    // Plant sapling
                    int saplingIndex = getSaplingIndex();
                    if (saplingIndex != -1) {
                        ItemStack saplingStack = this.golem.getInventory().getItem(saplingIndex);
                        if (saplingStack.getItem() instanceof BlockItem blockItem) {
                            Block saplingBlock = blockItem.getBlock();
                            
                            if (this.isGiant) {
                                // Pianta 4 sapling
                                for (int dx = 0; dx <= 1; dx++) {
                                    for (int dz = 0; dz <= 1; dz++) {
                                        BlockPos p = targetPos.offset(dx, 0, dz);
                                        golem.level().setBlock(p, saplingBlock.defaultBlockState(), 3);
                                    }
                                }
                                consumeItems(saplingStack.getItem(), 4);
                                this.golem.playSound(net.minecraft.sounds.SoundEvents.GRASS_PLACE, 1.0F, 1.0F);
                            } else {
                                // Pianta 1 sapling
                                net.minecraft.world.level.block.state.BlockState below = golem.level().getBlockState(targetPos.below());
                                net.minecraft.world.level.block.state.BlockState current = golem.level().getBlockState(targetPos);
                                if ((below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK)) && current.canBeReplaced()) {
                                    golem.level().setBlock(targetPos, saplingBlock.defaultBlockState(), 3);
                                    consumeItems(saplingStack.getItem(), 1);
                                    this.golem.playSound(net.minecraft.sounds.SoundEvents.GRASS_PLACE, 1.0F, 1.0F);
                                }
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

    private int countItem(net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeItems(net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < this.golem.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int toTake = Math.min(stack.getCount(), remaining);
                stack.shrink(toTake);
                remaining -= toTake;
            }
        }
    }
}
