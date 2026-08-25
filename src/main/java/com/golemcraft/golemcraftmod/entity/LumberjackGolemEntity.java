package com.golemcraft.golemcraftmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import com.golemcraft.golemcraftmod.entity.ai.ChopTreeGoal;
import com.golemcraft.golemcraftmod.entity.ai.DepositInChestGoal;
import com.golemcraft.golemcraftmod.entity.ai.PickupDroppedItemsGoal;
import com.golemcraft.golemcraftmod.entity.ai.PlantSaplingGoal;

public class LumberjackGolemEntity extends BaseGolemEntity {

    private static final EntityDataAccessor<BlockPos> CHEST_POS = SynchedEntityData.defineId(LumberjackGolemEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> IS_CHOPPING = SynchedEntityData.defineId(LumberjackGolemEntity.class, EntityDataSerializers.BOOLEAN);

    public LumberjackGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHEST_POS, BlockPos.ZERO);
        builder.define(IS_CHOPPING, false);
    }

    public BlockPos getChestPos() {
        return this.entityData.get(CHEST_POS);
    }

    public void setChestPos(BlockPos pos) {
        this.entityData.set(CHEST_POS, pos);
    }

    public boolean isChopping() {
        return this.entityData.get(IS_CHOPPING);
    }

    public void setChopping(boolean chopping) {
        this.entityData.set(IS_CHOPPING, chopping);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new DepositInChestGoal(this));
        this.goalSelector.addGoal(3, new ChopTreeGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new PlantSaplingGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new PickupDroppedItemsGoal(this, 1.1D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.getChestPos() != BlockPos.ZERO) {
            output.store("ChestPos", BlockPos.CODEC, this.getChestPos());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("ChestPos", BlockPos.CODEC).ifPresent(this::setChestPos);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            syncAxeToHand();
        }
    }

    private void syncAxeToHand() {
        ItemStack slot0 = this.getInventory().getItem(0);
        if (!slot0.isEmpty() && slot0.getItem() instanceof AxeItem) {
            this.setItemInHand(InteractionHand.MAIN_HAND, slot0.copy());
            this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
            this.getInventory().setItem(0, ItemStack.EMPTY);
        }
    }

    public boolean hasAxe() {
        ItemStack hand = this.getItemInHand(InteractionHand.MAIN_HAND);
        return !hand.isEmpty() && hand.getItem() instanceof AxeItem;
    }
}
