package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.block.AreaMarkerBlockEntity;
import com.golemcraft.golemcraftmod.entity.LevelerGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class LevelAreaGoal extends Goal {

    private final LevelerGolemEntity golem;
    private final double speed;

    private BlockPos targetPos;
    private boolean isPlacingBlock = false;
    private int breakTicks;
    private int scanCooldown = 0;

    public LevelAreaGoal(LevelerGolemEntity golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.golem.actionCooldown > 0) return false;
        if (this.golem.getOxidationLevel() == 3) return false;
        if (!this.golem.hasShovel()) return false;
        if (this.golem.isInventoryFull()) return false;

        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }

        return findWorkTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.golem.getOxidationLevel() == 3) return false;
        if (!this.golem.hasShovel()) return false;
        if (this.golem.isInventoryFull()) return false;
        return this.targetPos != null;
    }

    @Override
    public void start() {
        this.breakTicks = 0;
        if (this.targetPos != null) {
            this.golem.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, this.speed);
        }
    }

    @Override
    public void stop() {
        if (this.targetPos != null && this.golem.level() instanceof ServerLevel sl) {
            sl.destroyBlockProgress(this.golem.getId(), this.targetPos, -1);
        }
        this.targetPos = null;
        this.isPlacingBlock = false;
        this.breakTicks = 0;
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetPos == null) return;

        double distSq = this.golem.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 0.5D, this.targetPos.getZ() + 0.5D);

        this.golem.getLookControl().setLookAt(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 0.5D, this.targetPos.getZ() + 0.5D, 15.0F, 30.0F);

        if (distSq > 5.5D) {
            // Avvicinati al target
            if (this.golem.getNavigation().isDone() || this.golem.tickCount % 20 == 0) {
                double navSpeed = this.golem.isCharged() ? this.speed * 1.35D : this.speed;
                this.golem.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, navSpeed);
            }
        } else {
            // Siamo a portata di scavo/riempimento
            this.golem.getNavigation().stop();
            this.breakTicks++;

            if (this.golem.level() instanceof ServerLevel serverLevel) {
                if (!this.isPlacingBlock) {
                    BlockState state = serverLevel.getBlockState(this.targetPos);
                    int breakDuration = calculateBreakDuration(serverLevel, this.targetPos, state);

                    // Animazione braccio continuo durante lo scavo
                    this.golem.setAttackAnimTicks(5);

                    // Mostra crepe di scavo realistiche sul blocco
                    int stage = Math.min(9, (int) ((float) this.breakTicks / breakDuration * 10.0F));
                    serverLevel.destroyBlockProgress(this.golem.getId(), this.targetPos, stage);

                    int particleInterval = breakDuration <= 3 ? 1 : 2;
                    if (this.breakTicks % particleInterval == 0) {
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                this.targetPos.getX() + 0.5D, this.targetPos.getY() + 0.5D, this.targetPos.getZ() + 0.5D,
                                4, 0.15D, 0.15D, 0.15D, 0.05D);
                        serverLevel.playSound(null, this.targetPos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
                    }

                    if (this.breakTicks >= breakDuration) {
                        this.breakTicks = 0;
                        serverLevel.destroyBlockProgress(this.golem.getId(), this.targetPos, -1);
                        digBlock(serverLevel, this.targetPos, state);
                        collectDrops();
                        findWorkTarget(); // Seleziona il prossimo blocco
                    }
                } else {
                    // Riempimento buca
                    int maxPlace = this.golem.isCharged() ? 3 : 8;
                    if (this.breakTicks >= maxPlace) {
                        this.breakTicks = 0;
                        fillHole(serverLevel, this.targetPos);
                        findWorkTarget(); // Seleziona il prossimo blocco
                    }
                }
            }
        }
    }

    private int calculateBreakDuration(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(serverLevel, pos);
        if (hardness <= 0.0F) return 1;

        ItemStack shovel = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
        float speed = 1.0F;

        if (!shovel.isEmpty()) {
            speed = shovel.getDestroySpeed(state);

            // Calcola l'incantesimo Efficienza (Efficiency I..V)
            if (speed > 1.0F) {
                int efficiencyLevel = 0;
                ItemEnchantments enchants = shovel.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (var entry : enchants.entrySet()) {
                    if (entry.getKey().is(Enchantments.EFFICIENCY)) {
                        efficiencyLevel = entry.getIntValue();
                        break;
                    }
                }
                if (efficiencyLevel > 0) {
                    speed += (float) (efficiencyLevel * efficiencyLevel + 1);
                }
            }
        }

        // Effetto Fretta (Haste) da pozioni o Faro dei Golem
        if (this.golem.hasEffect(MobEffects.HASTE)) {
            int amp = this.golem.getEffect(MobEffects.HASTE).getAmplifier();
            speed *= (1.0F + (amp + 1) * 0.2F);
        }

        // Bonus Sovraccarico (Supercharged)
        if (this.golem.isCharged()) {
            speed *= 1.5F;
        }

        // Formula vanilla Minecraft: ticks = 30 * hardness / speed. Con fattore 20 per reattività golem
        int ticks = Math.round(20.0F * hardness / speed);
        return Math.max(1, ticks);
    }

    private void digBlock(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        if (isDiggable(serverLevel, pos, state)) {
            serverLevel.destroyBlock(pos, true, this.golem);

            // Usa l'attrezzo tramite logica vanilla (applica unbreaking e durabilità all'equipaggiamento)
            ItemStack shovel = this.golem.getItemInHand(InteractionHand.MAIN_HAND);
            if (!shovel.isEmpty() && shovel.isDamageableItem()) {
                shovel.hurtAndBreak(1, this.golem, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                if (shovel.isEmpty()) {
                    this.golem.playSound(SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
                    this.golem.syncShovelToHand();
                }
            }
        }
    }

    private void fillHole(ServerLevel serverLevel, BlockPos pos) {
        int fillerIndex = getFillerBlockIndex();
        if (fillerIndex != -1) {
            ItemStack stack = this.golem.getInventory().getItem(fillerIndex);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block blockToPlace = blockItem.getBlock();
                BlockState placeState = blockToPlace.defaultBlockState();
                serverLevel.setBlock(pos, placeState, Block.UPDATE_ALL);

                SoundType soundType = placeState.getSoundType();
                serverLevel.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                this.golem.setAttackAnimTicks(5);

                stack.shrink(1);
            }
        }
    }

    private boolean findWorkTarget() {
        Level level = this.golem.level();
        BlockPos currentPos = this.golem.blockPosition();

        AreaMarkerBlockEntity activeMarker = findNearestActiveMarker();
        if (activeMarker == null) {
            this.scanCooldown = 40;
            this.targetPos = null;
            return false;
        }

        AABB bounds = activeMarker.getWorkBounds();
        if (bounds == null) {
            this.scanCooldown = 40;
            this.targetPos = null;
            return false;
        }

        BlockPos marker1 = activeMarker.getBlockPos();
        BlockPos marker2 = activeMarker.getPartnerPos();

        int targetY = activeMarker.getTargetY();
        int minX = Math.min(marker1.getX(), marker2.getX());
        int maxX = Math.max(marker1.getX(), marker2.getX());
        int minZ = Math.min(marker1.getZ(), marker2.getZ());
        int maxZ = Math.max(marker1.getZ(), marker2.getZ());

        // 1. FASE DI SCAVO: cerca blocchi da scavare dall'alto verso il basso fino a quota targetY INCLUSA
        // (spiana allo stesso livello del marker, scavando anche i blocchi all'altezza del marker)
        BlockPos bestDig = null;
        int highestY = -999;
        double closestDistSq = Double.MAX_VALUE;

        int maxY = Math.min(targetY + 24, level.getMaxY());
        for (int y = maxY; y >= targetY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (p.equals(marker1) || p.equals(marker2)) continue;

                    BlockState st = level.getBlockState(p);
                    if (isDiggable(level, p, st)) {
                        double dSq = currentPos.distSqr(p);
                        if (y > highestY || (y == highestY && dSq < closestDistSq)) {
                            highestY = y;
                            closestDistSq = dSq;
                            bestDig = p;
                        }
                    }
                }
            }
            if (bestDig != null) {
                break; // Scava prima il layer più alto per evitare blocchi fluttuanti
            }
        }

        if (bestDig != null) {
            this.targetPos = bestDig;
            this.isPlacingBlock = false;
            return true;
        }

        // 2. FASE DI RIEMPIMENTO: se non ci sono più blocchi in rilievo, controlla buche sotto quota targetY
        if (hasFillerBlock()) {
            BlockPos bestHole = null;
            double closestHoleDistSq = Double.MAX_VALUE;

            int floorY = targetY - 1;
            for (int y = Math.max(level.getMinY(), floorY - 5); y <= floorY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (p.equals(marker1) || p.equals(marker2)) continue;

                        BlockState st = level.getBlockState(p);
                        if (st.canBeReplaced() || st.isAir()) {
                            // Verifica che ci sia un blocco solido sotto per poggiare il riempimento
                            BlockState below = level.getBlockState(p.below());
                            if (!below.isAir() && !below.canBeReplaced()) {
                                double dSq = currentPos.distSqr(p);
                                if (dSq < closestHoleDistSq) {
                                    closestHoleDistSq = dSq;
                                    bestHole = p;
                                }
                            }
                        }
                    }
                }
                if (bestHole != null) break; // Riempie dal basso verso l'alto
            }

            if (bestHole != null) {
                this.targetPos = bestHole;
                this.isPlacingBlock = true;
                return true;
            }
        }

        this.scanCooldown = 30;
        this.targetPos = null;
        return false;
    }

    private boolean isDiggable(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK)) return false;
        if (state.getBlock() instanceof com.golemcraft.golemcraftmod.block.AreaMarkerBlock) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        if (!state.getFluidState().isEmpty()) return false; // Non scava acqua o lava

        // Scava tutto ciò che richiede la pala o niente (mani nude)
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL) || !state.requiresCorrectToolForDrops();
    }

    private AreaMarkerBlockEntity findNearestActiveMarker() {
        Level level = this.golem.level();
        BlockPos currentPos = this.golem.blockPosition();

        AreaMarkerBlockEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.betweenClosed(currentPos.offset(-32, -16, -32), currentPos.offset(32, 16, 32))) {
            if (level.getBlockEntity(p) instanceof AreaMarkerBlockEntity marker && marker.hasPartner()) {
                double dSq = currentPos.distSqr(p);
                if (dSq < nearestDistSq) {
                    nearestDistSq = dSq;
                    nearest = marker;
                }
            }
        }
        return nearest;
    }

    private void collectDrops() {
        AABB aabb = this.golem.getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        List<ItemEntity> items = this.golem.level().getEntitiesOfClass(ItemEntity.class, aabb);
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                ItemStack remainder = this.golem.getInventory().addItem(stack.copy());
                if (remainder.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(remainder);
                }
            }
        }
        this.golem.setLastPickupTime(this.golem.level().getGameTime());
    }

    private boolean hasFillerBlock() {
        return getFillerBlockIndex() != -1;
    }

    private int getFillerBlockIndex() {
        for (int i = 0; i < this.golem.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.golem.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                Block block = bi.getBlock();
                if (block.defaultBlockState().is(BlockTags.DIRT) ||
                    block.defaultBlockState().is(BlockTags.SAND) ||
                    block == Blocks.GRAVEL ||
                    block == Blocks.COBBLESTONE) {
                    return i;
                }
            }
        }
        return -1;
    }
}
