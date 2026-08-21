package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import java.util.UUID;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;

public class BaseGolemEntity extends PathfinderMob implements ContainerUser {
    private static final EntityDataAccessor<Boolean> RUMMAGING = SynchedEntityData.defineId(BaseGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OXIDATION_LEVEL = SynchedEntityData.defineId(BaseGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_WAXED = SynchedEntityData.defineId(BaseGolemEntity.class, EntityDataSerializers.BOOLEAN);
    
    private final SimpleContainer inventory = new SimpleContainer(27);
    private UUID ownerUUID;
    private long lastPickupTime = 0;
    public int actionCooldown = 0;

    public BaseGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DOOR_OPEN, -1.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WALKABLE_DOOR, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RUMMAGING, false);
        builder.define(OXIDATION_LEVEL, 0);
        builder.define(IS_WAXED, false);
    }

    public void setRummaging(boolean rummaging) {
        this.entityData.set(RUMMAGING, rummaging);
    }

    public boolean isRummaging() {
        return this.entityData.get(RUMMAGING);
    }

    public int getOxidationLevel() {
        return this.entityData.get(OXIDATION_LEVEL);
    }

    public void setOxidationLevel(int level) {
        this.entityData.set(OXIDATION_LEVEL, net.minecraft.util.Mth.clamp(level, 0, 3));
    }

    public boolean isWaxed() {
        return this.entityData.get(IS_WAXED);
    }

    public void setWaxed(boolean waxed) {
        this.entityData.set(IS_WAXED, waxed);
    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public long getLastPickupTime() {
        return this.lastPickupTime;
    }

    public void setLastPickupTime(long time) {
        this.lastPickupTime = time;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.ownerUUID != null) {
            output.store("Owner", net.minecraft.core.UUIDUtil.CODEC, this.ownerUUID);
        }
        output.putLong("LastPickup", this.lastPickupTime);
        this.inventory.storeAsItemList(output.list("Inventory", net.minecraft.world.item.ItemStack.CODEC));
        output.store("Oxidation", com.mojang.serialization.Codec.INT, this.getOxidationLevel());
        output.store("Waxed", com.mojang.serialization.Codec.BOOL, this.isWaxed());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Owner", net.minecraft.core.UUIDUtil.CODEC).ifPresent(uuid -> this.ownerUUID = uuid);
        this.lastPickupTime = input.getLong("LastPickup").orElse(0L);
        input.list("Inventory", net.minecraft.world.item.ItemStack.CODEC).ifPresent(this.inventory::fromItemList);
        input.read("Oxidation", com.mojang.serialization.Codec.INT).ifPresent(this::setOxidationLevel);
        input.read("Waxed", com.mojang.serialization.Codec.BOOL).ifPresent(this::setWaxed);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level) {
            @Override
            protected net.minecraft.world.level.pathfinder.Path createPath(java.util.Set<net.minecraft.core.BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
                net.minecraft.world.level.pathfinder.Path path = super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange);
                if (path != null && containsGate(path)) return null;
                return path;
            }

            private boolean containsGate(net.minecraft.world.level.pathfinder.Path path) {
                for (int i = 0; i < path.getNodeCount(); i++) {
                    net.minecraft.core.BlockPos nodePos = path.getNode(i).asBlockPos();
                    if (level.getBlockState(nodePos).getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock ||
                        level.getBlockState(nodePos.below()).getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new com.golemcraft.golemcraftmod.entity.ai.StatueGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public double getContainerInteractionRange() {
        return 8.0D;
    }

    @Override
    public boolean hasContainerOpen(net.minecraft.world.level.block.entity.ContainerOpenersCounter openersCounter, net.minecraft.core.BlockPos pos) {
        return this.isRummaging(); 
    }

    @Override
    public LivingEntity getLivingEntity() {
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.actionCooldown > 0) {
            this.actionCooldown--;
        }
        
        if (!this.level().isClientSide()) {
            if (this.getOxidationLevel() == 3) {
                // Statue behavior: no movement, no AI ticking
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D)); // Gravity only
                this.getNavigation().stop();
                this.setYHeadRot(this.yBodyRot);
                this.setXRot(0);
            } else if (!this.isWaxed() && this.random.nextFloat() < 0.0005F) { // Very slow oxidation for testing
                this.setOxidationLevel(this.getOxidationLevel() + 1);
            }

            if (!(this instanceof com.golemcraft.golemcraftmod.entity.FarmerGolemEntity)) {
                ItemStack slot0 = this.inventory.getItem(0);
                ItemStack hand = this.getItemInHand(InteractionHand.MAIN_HAND);
                if (!ItemStack.isSameItemSameComponents(slot0, hand) || slot0.getCount() != hand.getCount()) {
                    this.setItemInHand(InteractionHand.MAIN_HAND, slot0.copy());
                }
            }
        }
    }



    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.getOxidationLevel() == 3) {
            super.travel(new net.minecraft.world.phys.Vec3(0, travelVector.y, 0));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && this.getOxidationLevel() < 3;
    }
    
    @Override
    protected void doPush(net.minecraft.world.entity.Entity entityIn) {
        if (this.getOxidationLevel() < 3) {
            super.doPush(entityIn);
        }
    }
    
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        if (this.getOxidationLevel() == 3) {
            if (damageSource.getEntity() instanceof Player player && player.getItemInHand(InteractionHand.MAIN_HAND).is(net.minecraft.tags.ItemTags.PICKAXES)) {
                // Drop as item
                net.minecraft.world.item.Item dropItem = com.golemcraft.golemcraftmod.registry.ModBlocks.BASE_GOLEM_ITEM.get();
                if (dropItem != net.minecraft.world.item.Items.AIR) {
                    this.spawnAtLocation(level, dropItem);
                }
                this.discard();
            }
            return false; // Statue is immune to other damage and acts like a block
        }
        return super.hurtServer(level, damageSource, amount);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(level, stack);
            }
        }
        
        ItemStack mainHand = this.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty()) {
            this.spawnAtLocation(level, mainHand);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return this.getOxidationLevel() == 3 ? null : net.minecraft.world.level.block.SoundType.COPPER.getStepSound(); 
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return net.minecraft.world.level.block.SoundType.COPPER.getHitSound();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.world.level.block.SoundType.COPPER.getBreakSound();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState blockState) {
        if (this.getOxidationLevel() < 3) {
            this.playSound(net.minecraft.world.level.block.SoundType.COPPER.getStepSound(), 0.15F, 1.0F);
        }
    }
    
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (this.ownerUUID == null) {
            this.ownerUUID = player.getUUID();
        }
        
        // Scraping with Axe
        if (itemstack.getItem() instanceof net.minecraft.world.item.AxeItem) {
            if (this.isWaxed()) {
                if (!this.level().isClientSide()) {
                    this.setWaxed(false);
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                    serverLevel.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 0.5D, this.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.0D);
                    this.playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.0F, 1.0F); 
                    itemstack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                }
                return InteractionResult.SUCCESS;
            } else if (this.getOxidationLevel() > 0) {
                if (!this.level().isClientSide()) {
                    this.setOxidationLevel(this.getOxidationLevel() - 1);
                    this.setNoAi(false); // Fix golems corrupted by the old isNoAi override
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                    serverLevel.sendParticles(ParticleTypes.SCRAPE, this.getX(), this.getY() + 0.5D, this.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.0D);
                    this.playSound(SoundEvents.AXE_SCRAPE, 1.0F, 1.0F);
                    itemstack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                }
                return InteractionResult.SUCCESS;
            }
        }
        
        // Waxing with Honeycomb
        if (itemstack.is(net.minecraft.world.item.Items.HONEYCOMB) && !this.isWaxed()) {
            if (!this.level().isClientSide()) {
                this.setWaxed(true);
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                serverLevel.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 0.5D, this.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.0D);
                this.playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // If statue, ignore other interactions
        if (this.getOxidationLevel() == 3) {
            return InteractionResult.PASS;
        }

        // Transform logic
        if (this.getType() == com.golemcraft.golemcraftmod.registry.ModEntities.BASE_GOLEM.get()) {
            if (itemstack.is(net.minecraft.tags.ItemTags.SMALL_FLOWERS) || itemstack.is(net.minecraft.tags.ItemTags.FLOWERS) || (itemstack.getItem() instanceof net.minecraft.world.item.BlockItem bi && (bi.getBlock() instanceof net.minecraft.world.level.block.FlowerBlock || bi.getBlock() instanceof net.minecraft.world.level.block.TallFlowerBlock))) {
                if (!this.level().isClientSide()) {
                    FlowerGolemEntity flowerGolem = ModEntities.FLOWER_GOLEM.get().create(this.level(), net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
                    if (flowerGolem != null) {
                        flowerGolem.setPos(this.getX(), this.getY(), this.getZ());
                        flowerGolem.setYRot(this.getYRot());
                        flowerGolem.setXRot(this.getXRot());
                        flowerGolem.setHealth(this.getHealth());
                        flowerGolem.yBodyRot = this.yBodyRot;
                        if (this.hasCustomName()) {
                            flowerGolem.setCustomName(this.getCustomName());
                            flowerGolem.setCustomNameVisible(this.isCustomNameVisible());
                        }
                        
                        flowerGolem.setOwnerUUID(this.ownerUUID);
                        flowerGolem.setLastPickupTime(this.lastPickupTime);
                        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                            flowerGolem.getInventory().setItem(i, this.inventory.getItem(i));
                        }
                        
                        this.level().addFreshEntity(flowerGolem);
                        
                        // Particles and sounds
                        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.0D);
                        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
                        
                        if (!player.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                        this.discard();
                    }
                }
                return InteractionResult.SUCCESS;
            }

            if (itemstack.getItem() instanceof net.minecraft.world.item.HoeItem) {
                if (!this.level().isClientSide()) {
                    FarmerGolemEntity farmerGolem = ModEntities.FARMER_GOLEM.get().create(this.level(), net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
                    if (farmerGolem != null) {
                        farmerGolem.setPos(this.getX(), this.getY(), this.getZ());
                        farmerGolem.setYRot(this.getYRot());
                        farmerGolem.setXRot(this.getXRot());
                        farmerGolem.setHealth(this.getHealth());
                        farmerGolem.yBodyRot = this.yBodyRot;
                        if (this.hasCustomName()) {
                            farmerGolem.setCustomName(this.getCustomName());
                            farmerGolem.setCustomNameVisible(this.isCustomNameVisible());
                        }

                        farmerGolem.setOwnerUUID(this.ownerUUID);
                        farmerGolem.setLastPickupTime(this.lastPickupTime);
                        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                            farmerGolem.getInventory().setItem(i, this.inventory.getItem(i));
                        }

                        // Equip the hoe directly to hand
                        ItemStack hoeStack = itemstack.copy();
                        hoeStack.setCount(1);
                        farmerGolem.setItemInHand(InteractionHand.MAIN_HAND, hoeStack);
                        farmerGolem.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);

                        this.level().addFreshEntity(farmerGolem);

                        // Particles and sounds
                        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.0D);
                        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);

                        if (!player.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                        this.discard();
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        
        InteractionResult interactionResult = super.mobInteract(player, hand);
        if (interactionResult.consumesAction()) {
            return interactionResult;
        }

        boolean isHoldingTool = itemstack.getItem() instanceof net.minecraft.world.item.AxeItem || 
                                itemstack.is(net.minecraft.world.item.Items.HONEYCOMB) ||
                                itemstack.is(net.minecraft.tags.ItemTags.HOES) ||
                                itemstack.is(net.minecraft.world.item.Items.LEAD) ||
                                itemstack.is(net.minecraft.tags.ItemTags.PICKAXES);

        // Require sneaking (Shift+Right Click) to open inventory, to completely avoid tool conflicts
        if (player.isSecondaryUseActive() && this.ownerUUID != null && this.ownerUUID.equals(player.getUUID())) {
            if (!this.level().isClientSide()) {
                player.openMenu(new SimpleMenuProvider(
                    (id, playerInv, p) -> ChestMenu.threeRows(id, playerInv, this.inventory),
                    this.getDisplayName()
                ));
            }
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }
}
