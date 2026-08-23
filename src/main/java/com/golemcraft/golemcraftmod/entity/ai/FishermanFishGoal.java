package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.FishermanGolemEntity;
import com.golemcraft.golemcraftmod.entity.projectile.GolemFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class FishermanFishGoal extends Goal {
    private final FishermanGolemEntity golem;
    private BlockPos targetWaterPos;
    private GolemFishingHook fishingHook;
    private int waitBeforeCasting;

    public FishermanFishGoal(FishermanGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK)); // No MOVE flag
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.golem.getOxidationLevel() == 3) return false;
        if (isInventoryFull()) return false;

        ItemStack handItem = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(handItem.getItem() instanceof FishingRodItem)) return false;

        return findWater();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.golem.getOxidationLevel() == 3) return false;
        ItemStack handItem = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(handItem.getItem() instanceof FishingRodItem)) return false;
        if (isInventoryFull()) return false;
        return this.targetWaterPos != null;
    }

    @Override
    public void start() {
        this.waitBeforeCasting = 20; 
        GolemCraft.LOGGER.debug("[FishermanGolem] Starting fishing goal, water={}", targetWaterPos);
        this.golem.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.golem.setFishing(false);
        this.targetWaterPos = null;
        if (this.fishingHook != null) {
            this.fishingHook.discard();
            this.fishingHook = null;
        }
    }

    @Override
    public void tick() {
        if (this.targetWaterPos == null) return;

        this.golem.getLookControl().setLookAt(
                this.targetWaterPos.getX() + 0.5D,
                this.targetWaterPos.getY() + 0.5D,
                this.targetWaterPos.getZ() + 0.5D);
        this.golem.setFishing(true);

        if (this.fishingHook == null) {
            if (this.waitBeforeCasting > 0) {
                this.waitBeforeCasting--;
            } else {
                castFishingLine();
            }
        } else {
            if (this.fishingHook.isRemoved()) {
                this.fishingHook = null;
                this.waitBeforeCasting = 40; 
            } else if (this.fishingHook.isBiting()) {
                catchFish();
                this.fishingHook.retrieve();
                this.fishingHook = null;
                
                this.golem.setFishing(false);
                this.targetWaterPos = null;
                this.golem.actionCooldown = 80;
            }
        }
    }

    private void castFishingLine() {
        if (!(this.golem.level() instanceof ServerLevel sl)) return;
        
        sl.playSound(null, this.golem.getX(), this.golem.getY(), this.golem.getZ(), 
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (this.golem.getRandom().nextFloat() * 0.4F + 0.8F));

        this.fishingHook = new GolemFishingHook(this.golem, sl, this.targetWaterPos);
        sl.addFreshEntity(this.fishingHook);
    }

    private void catchFish() {
        if (!(this.golem.level() instanceof ServerLevel serverLevel)) return;

        // Try vanilla fishing loot table first
        boolean lootSuccess = false;
        try {
            net.minecraft.world.level.storage.loot.LootTable lootTable =
                    serverLevel.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);

            LootParams lootParams = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, this.golem.position())
                    .withParameter(LootContextParams.TOOL, this.golem.getItemInHand(InteractionHand.MAIN_HAND))
                    .withParameter(LootContextParams.THIS_ENTITY, this.golem)
                    .create(LootContextParamSets.FISHING);

            List<ItemStack> drops = lootTable.getRandomItems(lootParams);
            GolemCraft.LOGGER.debug("[FishermanGolem] Loot table returned {} items", drops.size());

            if (!drops.isEmpty()) {
                for (ItemStack stack : drops) {
                    insertIntoInventory(stack);
                }
                lootSuccess = true;
            }
        } catch (Exception e) {
            GolemCraft.LOGGER.warn("[FishermanGolem] Loot table failed ({}), using fallback drops", e.getMessage());
        }

        // Fallback: manual weighted drops if loot table returned nothing
        if (!lootSuccess) {
            ItemStack drop = generateFishDrop();
            GolemCraft.LOGGER.debug("[FishermanGolem] Fallback drop: {}", drop);
            insertIntoInventory(drop);
        }

        this.golem.setLastPickupTime(serverLevel.getGameTime());

        // Consume rod durability
        ItemStack rod = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
        if (!rod.isEmpty() && rod.isDamageableItem()) {
            rod.hurtAndBreak(1, serverLevel, null, item -> {
                this.golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                this.golem.playSound(SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
                GolemCraft.LOGGER.debug("[FishermanGolem] Fishing rod broke!");
            });
        }
    }

    /** Weighted random fishing drop for the fallback system. */
    private ItemStack generateFishDrop() {
        float roll = this.golem.getRandom().nextFloat();
        // 60% fish, 20% junk, 20% treasure
        if (roll < 0.40f) {
            return new ItemStack(Items.COD, 1 + this.golem.getRandom().nextInt(3));
        } else if (roll < 0.60f) {
            return new ItemStack(Items.SALMON, 1 + this.golem.getRandom().nextInt(2));
        } else if (roll < 0.68f) {
            return new ItemStack(Items.TROPICAL_FISH);
        } else if (roll < 0.72f) {
            return new ItemStack(Items.PUFFERFISH);
        } else if (roll < 0.78f) {
            // Junk
            return junk();
        } else if (roll < 0.84f) {
            return new ItemStack(Items.STRING, 1 + this.golem.getRandom().nextInt(3));
        } else if (roll < 0.88f) {
            return new ItemStack(Items.BONE);
        } else if (roll < 0.91f) {
            return new ItemStack(Items.LILY_PAD);
        } else if (roll < 0.94f) {
            return new ItemStack(Items.SADDLE);
        } else if (roll < 0.97f) {
            return new ItemStack(Items.FISHING_ROD); // reward: new rod
        } else {
            return new ItemStack(Items.NAUTILUS_SHELL);
        }
    }

    private ItemStack junk() {
        int r = this.golem.getRandom().nextInt(4);
        return switch (r) {
            case 0 -> new ItemStack(Items.LEATHER);
            case 1 -> new ItemStack(Items.BOWL);
            case 2 -> new ItemStack(Items.INK_SAC);
            default -> new ItemStack(Items.BONE);
        };
    }

    private boolean findWater() {
        BlockPos currentPos = this.golem.blockPosition();
        Optional<BlockPos> nearestWater = BlockPos.betweenClosedStream(
                        currentPos.offset(-10, -3, -10),
                        currentPos.offset(10, 3, 10))
                .map(BlockPos::immutable)
                .filter(pos -> isWater(pos))
                .min(java.util.Comparator.comparingDouble(pos -> currentPos.distSqr(pos)));

        if (nearestWater.isPresent()) {
            this.targetWaterPos = nearestWater.get();
            return true;
        }
        return false;
    }

    private boolean isWater(BlockPos pos) {
        return this.golem.level().getFluidState(pos).is(Fluids.WATER)
                || this.golem.level().getBlockState(pos).is(Blocks.WATER);
    }

    private void insertIntoInventory(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = this.golem.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                this.golem.getInventory().setItem(i, stack.copy());
                return;
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space >= stack.getCount()) {
                    slotStack.grow(stack.getCount());
                    return;
                } else if (space > 0) {
                    slotStack.grow(space);
                    stack.shrink(space);
                }
            }
        }
        
        // If there's still remainder, drop it at the golem's feet
        if (!stack.isEmpty() && this.golem.level() instanceof ServerLevel sl) {
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                sl, this.golem.getX(), this.golem.getY(), this.golem.getZ(), stack
            );
            sl.addFreshEntity(itemEntity);
        }
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            if (this.golem.getInventory().getItem(i).isEmpty()) return false;
        }
        return true;
    }
}
