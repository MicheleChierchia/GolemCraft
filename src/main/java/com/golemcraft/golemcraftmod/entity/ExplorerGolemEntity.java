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
import net.minecraft.world.SimpleMenuProvider;
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

    /** The block where we last placed a LIGHT block (server-side only, not persisted). */
    private BlockPos lastLightBlockPos = null;

    /** Anchor position when staying/guarding */
    private BlockPos stayPos = null;

    /** Target death location for drop collection */
    private BlockPos deathPos = null;
    private int collectTimeout = 0;
    private int wakeUpCooldown = 0;
    private boolean hasOwnerDeparted = false;

    /** Countdown for updating the linked golem compass in owner's inventory. */
    private int compassUpdateTimer = 0;

    public ExplorerGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Synched data
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_WAITING, false);
        builder.define(IS_STAYING, false);
        builder.define(IS_COLLECTING_DROPS, false);
    }

    public boolean isWaiting() { return this.entityData.get(IS_WAITING); }

    public void setWaiting(boolean waiting) {
        this.entityData.set(IS_WAITING, waiting);
        if (!this.level().isClientSide()) {
            if (waiting) {
                this.getNavigation().stop();
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                updateLinkedCompasses();
            }
        }
    }

    public boolean isStaying() { return this.entityData.get(IS_STAYING); }

    public void setStaying(boolean staying) {
        this.entityData.set(IS_STAYING, staying);
        if (staying && !this.level().isClientSide()) {
            this.getNavigation().stop();
        }
    }

    public boolean isCollectingDrops() { return this.entityData.get(IS_COLLECTING_DROPS); }

    public void setCollectingDrops(boolean collecting) {
        this.entityData.set(IS_COLLECTING_DROPS, collecting);
    }

    public BlockPos getStayPos() { return this.stayPos; }
    public void setStayPos(BlockPos pos) { this.stayPos = pos; }

    // ─────────────────────────────────────────────────────────────────────────
    //  Attributes
    // ─────────────────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Goals
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new CollectDeathDropsGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.3D, 4.0F, 1.8F, 14.0F));
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Tick
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();

        // Always provide light around the golem (even while waiting/sitting)
        updateLightBlock(serverLevel);

        // Continuous owner status check: if owner died or is dying, initiate drop collection immediately
        Player owner = getOwnerPlayer();
        if (owner != null && (owner.isDeadOrDying() || !owner.isAlive())) {
            if (!isCollectingDrops() && !isWaiting()) {
                startCollectingDeathDrops(owner.blockPosition());
            }
        }

        // Proximity vacuum pickup while collecting drops
        if (isCollectingDrops()) {
            AABB reachBox = this.getBoundingBox().inflate(3.0D, 2.0D, 3.0D);
            List<ItemEntity> nearbyItems = serverLevel.getEntitiesOfClass(ItemEntity.class, reachBox,
                    item -> item.isAlive() && !item.getItem().isEmpty());
            for (ItemEntity itemEntity : nearbyItems) {
                ItemStack stack = itemEntity.getItem();
                int countBefore = stack.getCount();
                ItemStack remainder = this.getInventory().addItem(stack);
                if (remainder.isEmpty()) {
                    itemEntity.discard();
                    this.setLastPickupTime(serverLevel.getGameTime());
                    this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 0.9F + this.random.nextFloat() * 0.2F);
                } else {
                    itemEntity.setItem(remainder);
                    if (remainder.getCount() != countBefore) {
                        this.setLastPickupTime(serverLevel.getGameTime());
                        this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 0.9F + this.random.nextFloat() * 0.2F);
                    }
                }
            }
            this.setRummaging(true);
        }

        // Update linked golem compasses in owner inventory every 20 ticks
        compassUpdateTimer++;
        if (compassUpdateTimer >= 20) {
            compassUpdateTimer = 0;
            updateLinkedCompasses();
        }

        if (isWaiting()) {
            // Stay sitting still
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.getNavigation().stop();

            // Subtle particles around the sitting golem
            if (this.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                        getX() + (random.nextDouble() - 0.5D) * 0.4D,
                        getY() + 0.4D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.4D,
                        1, 0.0D, 0.02D, 0.0D, 0.01D);
            }

            if (wakeUpCooldown > 0) {
                wakeUpCooldown--;
            }

            // Proximity wake-up when owner returns:
            owner = getOwnerPlayer();
            if (owner != null && owner.isAlive() && !owner.isDeadOrDying() && !owner.isSpectator() && owner.getHealth() > 0.0F) {
                // If owner is far away (> 8 blocks), mark that owner has departed/respawned
                if (this.distanceToSqr(owner) > 64.0D) {
                    hasOwnerDeparted = true;
                }

                // If owner has departed (or cooldown expired and deathPos cleared) and is now close (< 4.5 blocks), wake up!
                if (wakeUpCooldown == 0 && (hasOwnerDeparted || this.deathPos == null) && this.distanceToSqr(owner) <= 20.25D) {
                    wakeUp(owner);
                }
            }
            return;
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isWaiting() || this.getOxidationLevel() == 3) {
            super.travel(new Vec3(0, travelVector.y, 0));
        } else {
            super.travel(travelVector);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Light block management
    // ─────────────────────────────────────────────────────────────────────────

    private void updateLightBlock(ServerLevel serverLevel) {
        if (this.isRemoved()) return;

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
        if (!this.level().isClientSide()) {
            removeLightBlock((ServerLevel) this.level());
        }
        super.remove(reason);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Compass tracking
    // ─────────────────────────────────────────────────────────────────────────

    private void updateLinkedCompasses() {
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

    public void startCollectingDeathDrops(BlockPos deathPos) {
        this.deathPos = deathPos;
        this.collectTimeout = 600; // 30 seconds maximum
        this.setCollectingDrops(true);
        this.setStaying(false);
        this.setWaiting(false);
        this.setRummaging(true);
        this.hasOwnerDeparted = false;
        this.wakeUpCooldown = 60;
        this.getNavigation().stop();
    }

    public void finishCollectingDrops() {
        this.setRummaging(false);
        this.setCollectingDrops(false);
        this.setWaiting(true);
        this.wakeUpCooldown = 60;
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    25, 0.3D, 0.3D, 0.3D, 0.02D);
            this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 0.8F);
        }
        updateLinkedCompasses();
    }

    private void wakeUp(Player owner) {
        setWaiting(false);
        this.deathPos = null;
        this.hasOwnerDeparted = false;
        this.wakeUpCooldown = 0;
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 0.8D, this.getZ(),
                    15, 0.3D, 0.3D, 0.3D, 0.05D);
            sl.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + 1.2D, this.getZ(),
                    5, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Interaction
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. Shift + Right Click opens the 27-slot inventory GUI (both when sitting and when awake)
        if (player.isSecondaryUseActive() && (this.getOwnerUUID() == null || this.getOwnerUUID().equals(player.getUUID()))) {
            if (!this.level().isClientSide()) {
                player.openMenu(new SimpleMenuProvider(
                    (id, playerInv, p) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, playerInv, this.getInventory()),
                    this.getDisplayName()
                ));
            }
            return InteractionResult.SUCCESS;
        }

        // 2. If waiting/dormant, right-click wakes up the golem
        if (isWaiting()) {
            if (this.getOwnerUUID() == null || this.getOwnerUUID().equals(player.getUUID())) {
                if (!this.level().isClientSide()) {
                    wakeUp(player);
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
            // Allow owner with pickaxe to pick up the golem
            net.minecraft.world.entity.Entity attacker = damageSource.getEntity();
            if (attacker instanceof Player player
                    && (this.getOwnerUUID() == null || player.getUUID().equals(this.getOwnerUUID()))
                    && player.getItemInHand(InteractionHand.MAIN_HAND).is(net.minecraft.tags.ItemTags.PICKAXES)) {
                wakeUp(player);
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
        private int noItemsTicks = 0;

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
            noItemsTicks = 0;
        }

        @Override
        public void stop() {
            targetItem = null;
            noItemsTicks = 0;
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            golem.collectTimeout--;

            if (golem.collectTimeout <= 0 || golem.isInventoryFull()) {
                golem.finishCollectingDrops();
                return;
            }

            targetItem = findNearestDrop();
            if (targetItem != null && targetItem.isAlive()) {
                noItemsTicks = 0;
                golem.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);
                golem.getNavigation().moveTo(targetItem, speed);
            } else {
                noItemsTicks++;
                // If no items found for 5 consecutive ticks (0.25 seconds), we are finished!
                if (noItemsTicks >= 5) {
                    golem.finishCollectingDrops();
                    return;
                }
                if (golem.deathPos != null && golem.distanceToSqr(Vec3.atBottomCenterOf(golem.deathPos)) > 4.0D) {
                    golem.getNavigation().moveTo(golem.deathPos.getX() + 0.5D, golem.deathPos.getY(), golem.deathPos.getZ() + 0.5D, speed);
                }
            }
        }

        private ItemEntity findNearestDrop() {
            BlockPos center = golem.deathPos != null ? golem.deathPos : golem.blockPosition();
            AABB box = new AABB(center).inflate(36.0D, 16.0D, 36.0D);
            List<ItemEntity> items = golem.level().getEntitiesOfClass(ItemEntity.class, box,
                    item -> item.isAlive() && !item.getItem().isEmpty());
            if (items.isEmpty()) return null;

            return items.stream().min(java.util.Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
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
            if (p == null || !p.isAlive() || p.isSpectator() || p.isDeadOrDying()) return false;
            if (golem.distanceToSqr(p) < (double) startDistSq) return false;
            owner = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.isStaying() || golem.isWaiting() || golem.isCollectingDrops() || golem.getOxidationLevel() == 3) return false;
            if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.isDeadOrDying()) return false;
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
            if (owner == null || !owner.isAlive() || owner.isDeadOrDying() || golem.isWaiting() || golem.isCollectingDrops() || golem.isStaying()) return;

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
            // Teleport strictly during normal exploration - NEVER when dormant/waiting, collecting drops, staying, or if player is dead/dying!
            if (golem.isWaiting() || golem.isCollectingDrops() || golem.isStaying()) return;
            if (!target.isAlive() || target.isSpectator() || target.isDeadOrDying()) return;

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
