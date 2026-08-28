package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.item.GolemCompassItem;
import com.golemcraft.golemcraftmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class ExplorerGolemEntity extends BaseGolemEntity {

    private static final EntityDataAccessor<Boolean> IS_WAITING =
            SynchedEntityData.defineId(ExplorerGolemEntity.class, EntityDataSerializers.BOOLEAN);

    /** The block where we last placed a LIGHT block (server-side only, not persisted). */
    private BlockPos lastLightBlockPos = null;

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
    }

    public boolean isWaiting() { return this.entityData.get(IS_WAITING); }

    public void setWaiting(boolean waiting) {
        this.entityData.set(IS_WAITING, waiting);
        if (waiting && !this.level().isClientSide()) {
            this.getNavigation().stop();
        }
    }

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
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.3D, 4.0F, 1.8F, 16.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
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

        if (isWaiting()) {
            // Sitting still
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

            // Check if owner is back and alive nearby
            Player owner = getOwnerPlayer();
            if (owner != null && owner.isAlive() && this.distanceToSqr(owner) < 25.0D) {
                returnItemsToOwner(owner);
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Light block management
    // ─────────────────────────────────────────────────────────────────────────

    private void updateLightBlock(ServerLevel serverLevel) {
        if (this.isRemoved()) return;

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
            if (myUUID.equals(tag.getString(GolemCompassItem.GOLEM_UUID_KEY))) {
                stack.set(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER, tracker);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Death drop collection / waiting state
    // ─────────────────────────────────────────────────────────────────────────

    private void returnItemsToOwner(Player owner) {
        SimpleContainer inv = getInventory();
        boolean returnedAny = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (owner.getInventory().add(stack.copy())) {
                inv.setItem(i, ItemStack.EMPTY);
            } else {
                owner.drop(stack.copy(), false);
                inv.setItem(i, ItemStack.EMPTY);
            }
            returnedAny = true;
        }
        setWaiting(false);
        if (returnedAny) {
            owner.sendSystemMessage(Component.translatable("entity.golemcraft.explorer_golem.returned_items"));
        }
        this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Interaction
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Compass (normal or recovery) → create a GolemCompass item
        if (!this.level().isClientSide()
                && (itemstack.is(Items.COMPASS) || itemstack.is(Items.RECOVERY_COMPASS))
                && this.getOxidationLevel() < 3 && !isWaiting()) {

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

            // Consume the compass (unless creative)
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            player.addItem(compassOut);
            this.playSound(SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        // If waiting, only allow owner to interact (returns items)
        if (isWaiting()) {
            if (this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID())) {
                if (!this.level().isClientSide()) {
                    returnItemsToOwner(player);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        return super.mobInteract(player, hand);
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
                    && player.getUUID().equals(this.getOwnerUUID())
                    && player.getItemInHand(InteractionHand.MAIN_HAND).is(net.minecraft.tags.ItemTags.PICKAXES)) {
                // Return items first, then pick up
                returnItemsToOwner(player);
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
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("IsWaiting", com.mojang.serialization.Codec.BOOL).ifPresent(this::setWaiting);
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
    //  Inner goal: FollowOwnerGoal
    // ─────────────────────────────────────────────────────────────────────────

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
            if (golem.isWaiting() || golem.getOxidationLevel() == 3) return false;
            Player p = golem.getOwnerPlayer();
            if (p == null || !p.isAlive() || p.isSpectator()) return false;
            if (golem.distanceToSqr(p) < (double) startDistSq) return false;
            owner = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.isWaiting() || golem.getOxidationLevel() == 3) return false;
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
