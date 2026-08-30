package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.item.GolemCompassItem;
import com.golemcraft.golemcraftmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ExplorerGolemEntity extends BaseGolemEntity {

    private static final EntityDataAccessor<Boolean> IS_WAITING =
            SynchedEntityData.defineId(ExplorerGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_STAYING =
            SynchedEntityData.defineId(ExplorerGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_COLLECTING_DROPS =
            SynchedEntityData.defineId(ExplorerGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private BlockPos stayPos;
    private BlockPos deathPos;
    private int collectTimeout = 0;
    private BlockPos lastLightBlockPos = null;
    private int compassUpdateTimer = 0;

    public ExplorerGolemEntity(EntityType<? extends BaseGolemEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_WAITING, false);
        builder.define(IS_STAYING, false);
        builder.define(IS_COLLECTING_DROPS, false);
    }

    public boolean isWaiting() {
        return this.entityData.get(IS_WAITING);
    }

    public void setWaiting(boolean waiting) {
        this.entityData.set(IS_WAITING, waiting);
    }

    public boolean isStaying() {
        return this.entityData.get(IS_STAYING);
    }

    public void setStaying(boolean staying) {
        this.entityData.set(IS_STAYING, staying);
    }

    public boolean isCollectingDrops() {
        return this.entityData.get(IS_COLLECTING_DROPS);
    }

    public void setCollectingDrops(boolean collecting) {
        this.entityData.set(IS_COLLECTING_DROPS, collecting);
    }

    public BlockPos getStayPos() {
        return stayPos;
    }

    public void setStayPos(BlockPos pos) {
        this.stayPos = pos;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Goals
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new CollectDeathDropsGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.3D, 4.0F, 1.8F, 16.0F));
        this.goalSelector.addGoal(3, new ReturnToStayPositionGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override
            public boolean canUse() {
                if (ExplorerGolemEntity.this.isStaying() || ExplorerGolemEntity.this.isWaiting() || ExplorerGolemEntity.this.isCollectingDrops()) {
                    return false;
                }
                return super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.getOxidationLevel() < 3) {
                updateLightBlock(serverLevel);
            } else {
                removeLightBlock(serverLevel);
            }

            if (isWaiting()) {
                this.getNavigation().stop();

                // Subtle particles around the sitting golem
                if (this.tickCount % 10 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                            getX() + (random.nextDouble() - 0.5D) * 0.4D,
                            getY() + 0.4D,
                            getZ() + (random.nextDouble() - 0.5D) * 0.4D,
                            1, 0.0D, 0.02D, 0.0D, 0.01D);
                }

                // Check if owner is back and alive nearby - wake up and resume following without dumping items
                Player owner = getOwnerPlayer();
                if (owner != null && owner.isAlive() && this.distanceToSqr(owner) < 25.0D) {
                    this.setWaiting(false);
                    this.setStaying(false);
                    this.setStayPos(null);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                }
                return;
            }

            // Update linked golem compasses in owner inventory every 20 ticks
            compassUpdateTimer++;
            if (compassUpdateTimer >= 20) {
                compassUpdateTimer = 0;
                updateLinkedCompasses();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Light block management
    // ─────────────────────────────────────────────────────────────────────────

    private void updateLightBlock(ServerLevel serverLevel) {
        BlockPos currentPos = this.blockPosition();
        if (currentPos.equals(lastLightBlockPos)) return;
        if (this.isRemoved()) return;

        removeLightBlock(serverLevel);
        // Find the best air block around the golem (torso -> head -> feet -> above)
        BlockPos torsoPos = BlockPos.containing(this.getX(), this.getY() + 0.5D, this.getZ());
        BlockPos targetPos = null;

        BlockPos[] candidates = new BlockPos[]{
                torsoPos,
                torsoPos.above(),
                this.blockPosition(),
                this.blockPosition().above(2)
        };

        for (BlockPos pos : candidates) {
            BlockState st = serverLevel.getBlockState(pos);
            if (st.isAir() || st.is(Blocks.LIGHT)) {
                targetPos = pos;
                break;
            }
        }

        if (targetPos == null) return;

        if (targetPos.equals(lastLightBlockPos)) {
            BlockState st = serverLevel.getBlockState(targetPos);
            if (st.isAir()) {
                serverLevel.setBlock(targetPos,
                        Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15),
                        Block.UPDATE_ALL_IMMEDIATE);
            }
            return;
        }

        // Place new light first
        serverLevel.setBlock(targetPos,
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15),
                Block.UPDATE_ALL_IMMEDIATE);

        // Then remove old light block
        if (lastLightBlockPos != null && !lastLightBlockPos.equals(targetPos)) {
            BlockState oldState = serverLevel.getBlockState(lastLightBlockPos);
            if (oldState.is(Blocks.LIGHT)) {
                serverLevel.setBlock(lastLightBlockPos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);
            }
        }
        lastLightBlockPos = targetPos;
    }

    private void removeLightBlock(ServerLevel serverLevel) {
        if (lastLightBlockPos != null) {
            BlockState state = serverLevel.getBlockState(lastLightBlockPos);
            if (state.is(Blocks.LIGHT)) {
                serverLevel.setBlock(lastLightBlockPos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);
            }
            lastLightBlockPos = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.level() instanceof ServerLevel serverLevel) {
            removeLightBlock(serverLevel);
        }
        super.remove(reason);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Linked compasses
    // ─────────────────────────────────────────────────────────────────────────

    public void updateLinkedCompasses() {
        Player owner = getOwnerPlayer();
        if (owner == null) return;

        String myUUID = this.getUUID().toString();
        GlobalPos globalPos = GlobalPos.of(this.level().dimension(), this.blockPosition());
        LodestoneTracker tracker = new LodestoneTracker(Optional.of(globalPos), false);

        // Scan main inventory (36 slots)
        net.minecraft.world.entity.player.Inventory inv = owner.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof GolemCompassItem)) continue;
            CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            CompoundTag tag = data.copyTag();
            tag.getString(GolemCompassItem.GOLEM_UUID_KEY).ifPresent(uuidStr -> {
                if (myUUID.equals(uuidStr)) {
                    stack.set(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER, tracker);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Drop collection and waiting state
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isInventoryFull() {
        SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void collectDropsFromDeath(BlockPos deathPos, java.util.Collection<ItemEntity> drops) {
        this.deathPos = deathPos;
        this.setCollectingDrops(false);
        this.setStaying(true);
        this.setStayPos(deathPos);

        // Move directly to the death spot
        this.teleportTo(deathPos.getX() + 0.5D, deathPos.getY(), deathPos.getZ() + 0.5D);
        this.getNavigation().stop();

        // Collect player's death drops directly into internal 27-slot inventory
        if (drops != null) {
            java.util.Iterator<ItemEntity> it = drops.iterator();
            while (it.hasNext()) {
                ItemEntity itemEntity = it.next();
                if (itemEntity != null && itemEntity.isAlive()) {
                    ItemStack stack = itemEntity.getItem();
                    if (!stack.isEmpty()) {
                        ItemStack remainder = this.getInventory().addItem(stack.copy());
                        if (remainder.isEmpty()) {
                            itemEntity.discard();
                            it.remove();
                        } else {
                            itemEntity.setItem(remainder);
                        }
                    }
                }
            }
        }

        // Set waiting state at death location
        this.setWaiting(true);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    25, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        updateLinkedCompasses();
    }

    public void startCollectingDeathDrops(BlockPos deathPos) {
        this.deathPos = deathPos;
        this.collectTimeout = 600; // 30 seconds maximum
        this.setCollectingDrops(true);
        this.setStaying(false);
        this.setWaiting(false);

        if (this.distanceToSqr(Vec3.atCenterOf(deathPos)) > 256.0D) {
            this.teleportTo(deathPos.getX() + 0.5D, deathPos.getY(), deathPos.getZ() + 0.5D);
        }
    }

    public void finishCollectingDrops() {
        this.setCollectingDrops(false);
        this.setWaiting(true);
        this.setStaying(true);
        if (this.deathPos != null) {
            this.setStayPos(this.deathPos);
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    20, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        updateLinkedCompasses();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Interaction
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. If waiting, allow Shift+Right Click to open inventory, or regular Right-Click to wake up & resume following
        if (isWaiting()) {
            if (player.isSecondaryUseActive()) {
                return super.mobInteract(player, hand);
            }
            if (this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID())) {
                if (!this.level().isClientSide()) {
                    this.setWaiting(false);
                    this.setStaying(false);
                    this.setStayPos(null);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // 2. Compass (normal, recovery, or existing golem compass) → create/link a GolemCompass item
        if (!this.level().isClientSide()
                && (itemstack.is(Items.COMPASS) || itemstack.is(Items.RECOVERY_COMPASS) || itemstack.is(ModBlocks.GOLEM_COMPASS_ITEM.get()))
                && this.getOxidationLevel() < 3 && !isWaiting()) {

            if (this.getOwnerUUID() == null) {
                this.setOwnerUUID(player.getUUID());
            }

            // Build the GolemCompass item
            ItemStack compassOut = new ItemStack(ModBlocks.GOLEM_COMPASS_ITEM.get());

            // Store our UUID in CUSTOM_DATA
            CompoundTag tag = new CompoundTag();
            tag.putString(GolemCompassItem.GOLEM_UUID_KEY, this.getUUID().toString());
            compassOut.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));

            // Set initial lodestone position immediately
            GlobalPos globalPos = GlobalPos.of(this.level().dimension(), this.blockPosition());
            compassOut.set(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.of(globalPos), false));

            // Consume the item in hand and give the golem compass
            ItemStack result = net.minecraft.world.item.ItemUtils.createFilledResult(itemstack, player, compassOut);
            player.setItemInHand(hand, result);

            this.playSound(SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F, 1.2F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 0.8D, this.getZ(),
                        8, 0.2D, 0.2D, 0.2D, 0.05D);
            }
            player.sendSystemMessage(Component.translatable("entity.golemcraft.explorer_golem.compass_linked"));
            return InteractionResult.SUCCESS;
        }

        // 3. Let BaseGolemEntity handle axes (scraping/waxing), honeycomb, or Shift+Click inventory menu
        InteractionResult baseResult = super.mobInteract(player, hand);
        if (baseResult.consumesAction()) {
            return baseResult;
        }

        if (this.getOxidationLevel() == 3) {
            return InteractionResult.PASS;
        }

        // 4. Empty hand click (non-shift) toggles Follow vs Stay mode
        if (itemstack.isEmpty() && !player.isSecondaryUseActive()) {
            if (this.getOwnerUUID() == null) {
                this.setOwnerUUID(player.getUUID());
            }
            if (this.getOwnerUUID().equals(player.getUUID())) {
                if (!this.level().isClientSide()) {
                    boolean nextState = !this.isStaying();
                    this.setStaying(nextState);
                    if (nextState) {
                        this.setStayPos(this.blockPosition());
                        this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.0F);
                        if (this.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
                        }
                    } else {
                        this.setStayPos(null);
                        this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
                        if (this.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Intangible / invulnerable when waiting
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean isPushable() {
        return !isWaiting() && super.isPushable();
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (!isWaiting()) super.doPush(entity);
    }

    @Override
    public boolean isPickable() {
        return !isWaiting();
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        if (isWaiting()) {
            net.minecraft.world.entity.Entity attacker = damageSource.getEntity();
            if (attacker instanceof Player player
                    && player.getUUID().equals(this.getOwnerUUID())
                    && player.getItemInHand(InteractionHand.MAIN_HAND).is(net.minecraft.tags.ItemTags.PICKAXES)) {
                this.setWaiting(false);
                this.setStaying(false);
                this.setStayPos(null);
            }
            return false; // otherwise immune
        }
        return super.hurtServer(level, damageSource, amount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Save / Load
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("IsWaiting", com.mojang.serialization.Codec.BOOL, this.isWaiting());
        output.store("IsStaying", com.mojang.serialization.Codec.BOOL, this.isStaying());
        if (this.stayPos != null) {
            output.store("StayPos", BlockPos.CODEC, this.stayPos);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("IsWaiting", com.mojang.serialization.Codec.BOOL).ifPresent(this::setWaiting);
        input.read("IsStaying", com.mojang.serialization.Codec.BOOL).ifPresent(this::setStaying);
        input.read("StayPos", BlockPos.CODEC).ifPresent(this::setStayPos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    public Player getOwnerPlayer() {
        UUID ownerUUID = this.getOwnerUUID();
        if (ownerUUID == null) return null;
        return this.level().getPlayerByUUID(ownerUUID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner goals
    // ─────────────────────────────────────────────────────────────────────────

    private static class CollectDeathDropsGoal extends Goal {
        private final ExplorerGolemEntity golem;
        private final double speed;
        private ItemEntity targetItem;
        private int noItemTicks = 0;

        public CollectDeathDropsGoal(ExplorerGolemEntity golem, double speed) {
            this.golem = golem;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return golem.isCollectingDrops() && !golem.isWaiting() && golem.getOxidationLevel() < 3;
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isCollectingDrops() && !golem.isWaiting() && golem.getOxidationLevel() < 3;
        }

        @Override
        public void start() {
            targetItem = null;
            noItemTicks = 0;
        }

        @Override
        public void stop() {
            targetItem = null;
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            golem.collectTimeout--;

            if (golem.isInventoryFull() || golem.collectTimeout <= 0) {
                golem.finishCollectingDrops();
                return;
            }

            if (targetItem == null || !targetItem.isAlive()) {
                targetItem = findNearestDrop();
                if (targetItem == null) {
                    if (golem.deathPos != null && golem.distanceToSqr(Vec3.atCenterOf(golem.deathPos)) > 9.0D) {
                        golem.getNavigation().moveTo(golem.deathPos.getX() + 0.5D, golem.deathPos.getY(), golem.deathPos.getZ() + 0.5D, speed);
                    } else {
                        noItemTicks++;
                        if (noItemTicks > 40) {
                            golem.finishCollectingDrops();
                            return;
                        }
                    }
                } else {
                    noItemTicks = 0;
                    golem.getNavigation().moveTo(targetItem, speed);
                }
            }

            if (targetItem != null && targetItem.isAlive()) {
                golem.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);

                if (golem.distanceToSqr(targetItem) < 9.0D) {
                    ItemStack stack = targetItem.getItem();
                    if (!stack.isEmpty()) {
                        ItemStack remainder = golem.getInventory().addItem(stack.copy());
                        if (remainder.isEmpty()) {
                            targetItem.discard();
                            golem.setLastPickupTime(golem.level().getGameTime());
                        } else {
                            targetItem.setItem(remainder);
                            if (stack.getCount() != remainder.getCount()) {
                                golem.setLastPickupTime(golem.level().getGameTime());
                            }
                        }
                        golem.playSound(SoundEvents.ITEM_PICKUP, 0.5F, (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F + 1.0F);
                    }
                    targetItem = null;
                } else if (golem.getNavigation().isDone()) {
                    golem.getNavigation().moveTo(targetItem, speed);
                }
            }
        }

        private ItemEntity findNearestDrop() {
            BlockPos center = golem.deathPos != null ? golem.deathPos : golem.blockPosition();
            AABB box = new AABB(center).inflate(32.0D, 16.0D, 32.0D);
            List<ItemEntity> items = golem.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive);
            if (items.isEmpty()) return null;

            return items.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        }
    }

    private static class ReturnToStayPositionGoal extends Goal {
        private final ExplorerGolemEntity golem;
        private final double speed;

        public ReturnToStayPositionGoal(ExplorerGolemEntity golem, double speed) {
            this.golem = golem;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!golem.isStaying() || golem.isWaiting() || golem.isCollectingDrops() || golem.getStayPos() == null) return false;
            return golem.distanceToSqr(Vec3.atBottomCenterOf(golem.getStayPos())) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            if (!golem.isStaying() || golem.isWaiting() || golem.isCollectingDrops() || golem.getStayPos() == null) return false;
            return !golem.getNavigation().isDone() && golem.distanceToSqr(Vec3.atBottomCenterOf(golem.getStayPos())) > 1.5D;
        }

        @Override
        public void start() {
            if (golem.distanceToSqr(Vec3.atBottomCenterOf(golem.getStayPos())) > 144.0D) {
                golem.teleportTo(golem.getStayPos().getX() + 0.5D, golem.getStayPos().getY(), golem.getStayPos().getZ() + 0.5D);
                golem.getNavigation().stop();
            } else {
                golem.getNavigation().moveTo(golem.getStayPos().getX() + 0.5D, golem.getStayPos().getY(), golem.getStayPos().getZ() + 0.5D, speed);
            }
        }
    }

    private static class FollowOwnerGoal extends Goal {
        private final ExplorerGolemEntity golem;
        private final double speed;
        private final float startDistSq;
        private final float stopDistSq;
        private final float teleportDistSq;
        private int recalcTimer;
        private Player owner;

        public FollowOwnerGoal(ExplorerGolemEntity golem, double speed, float startDist, float stopDist, float teleportDist) {
            this.golem = golem;
            this.speed = speed;
            this.startDistSq = startDist * startDist;
            this.stopDistSq = stopDist * stopDist;
            this.teleportDistSq = teleportDist * teleportDist;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.isStaying() || golem.isWaiting() || golem.isCollectingDrops() || golem.getOxidationLevel() == 3) return false;
            Player p = golem.getOwnerPlayer();
            if (p == null || !p.isAlive() || p.isSpectator()) return false;
            if (golem.distanceToSqr(p) < (double) startDistSq) return false;
            owner = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.isStaying() || golem.isWaiting() || golem.isCollectingDrops() || golem.getOxidationLevel() == 3) return false;
            if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;
            return golem.distanceToSqr(owner) > (double) stopDistSq;
        }

        @Override
        public void start() { recalcTimer = 0; }

        @Override
        public void stop() {
            golem.getNavigation().stop();
            owner = null;
        }

        @Override
        public void tick() {
            owner = golem.getOwnerPlayer();
            if (owner == null) return;

            golem.getLookControl().setLookAt(owner, 10.0F, golem.getMaxHeadXRot());

            if (--recalcTimer > 0) return;
            recalcTimer = 10;

            double distSq = golem.distanceToSqr(owner);
            if (distSq > (double) teleportDistSq) {
                teleportNear(owner);
            } else if (distSq > (double) stopDistSq) {
                golem.getNavigation().moveTo(owner, speed);
            } else {
                golem.getNavigation().stop();
            }
        }

        private void teleportNear(Player target) {
            net.minecraft.util.RandomSource rng = golem.getRandom();
            for (int i = 0; i < 10; i++) {
                double ox = target.getX() + (rng.nextFloat() * 6.0F - 3.0F);
                double oz = target.getZ() + (rng.nextFloat() * 6.0F - 3.0F);
                BlockPos g = BlockPos.containing(ox, target.getY() + 2, oz);
                int tries = 6;
                while (tries-- > 0 && !golem.level().getBlockState(g).isSolid()) {
                    g = g.below();
                }

                if (golem.level().getBlockState(g).isSolid()) {
                    BlockPos tpPos = g.above();
                    if (golem.level().noCollision(golem, golem.getBoundingBox().move(
                            ox - golem.getX(), tpPos.getY() - golem.getY(), oz - golem.getZ()))) {
                        golem.teleportTo(ox, tpPos.getY(), oz);
                        golem.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }
}
