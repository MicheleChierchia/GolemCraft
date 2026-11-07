package com.golemcraft.golemcraftmod.entity;

import com.golemcraft.golemcraftmod.entity.ai.BeekeeperHarvestGoal;
import com.golemcraft.golemcraftmod.entity.ai.BeekeeperPatrolGoal;
import com.golemcraft.golemcraftmod.entity.ai.BeekeeperShootHoneyGoal;
import com.golemcraft.golemcraftmod.entity.ai.DepositInChestGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BeekeeperGolemEntity extends BaseGolemEntity {
    private static final EntityDataAccessor<Integer> HONEY_LEVEL =
            SynchedEntityData.defineId(BeekeeperGolemEntity.class, EntityDataSerializers.INT);
    private static EntityDataAccessor<Byte> BEE_DATA_FLAGS_ID = null;

    static {
        try {
            java.lang.reflect.Field field = Bee.class.getDeclaredField("DATA_FLAGS_ID");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            EntityDataAccessor<Byte> accessor = (EntityDataAccessor<Byte>) field.get(null);
            BEE_DATA_FLAGS_ID = accessor;
        } catch (Exception ignored) {
        }
    }

    public BeekeeperGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HONEY_LEVEL, 0);
    }

    public int getHoneyLevel() {
        return this.entityData.get(HONEY_LEVEL);
    }

    public void setHoneyLevel(int level) {
        this.entityData.set(HONEY_LEVEL, Math.max(0, Math.min(5, level)));
    }

    public void incrementHoneyLevel() {
        setHoneyLevel(getHoneyLevel() + 1);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BeekeeperShootHoneyGoal(this));
        this.goalSelector.addGoal(2, new BeekeeperHarvestGoal(this));
        this.goalSelector.addGoal(3, new DepositInChestGoal(this));
        this.goalSelector.addGoal(4, new BeekeeperPatrolGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Visual dripping honey particles when honey level > 0
        int honey = getHoneyLevel();
        if (honey > 0 && this.level().isClientSide()) {
            int chance = Math.max(1, 8 - honey);
            if (this.random.nextInt(chance) == 0) {
                this.level().addParticle(ParticleTypes.DRIPPING_HONEY,
                        this.getRandomX(0.7D),
                        this.getY() + 0.4D + this.random.nextDouble() * 0.6D,
                        this.getRandomZ(0.7D),
                        0.0D, 0.0D, 0.0D);
            }
            if (honey >= 3 && this.random.nextInt(8) == 0) {
                this.level().addParticle(ParticleTypes.FALLING_HONEY,
                        this.getRandomX(0.5D),
                        this.getY() + 0.8D,
                        this.getRandomZ(0.5D),
                        0.0D, -0.05D, 0.0D);
            }
        }

        // Pollination logic: attract nearby bees searching for nectar and pollinate them
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            // Attract bees looking for flowers within 10 blocks
            AABB attractionZone = this.getBoundingBox().inflate(10.0D, 4.0D, 10.0D);
            List<Bee> attractableBees = this.level().getEntitiesOfClass(Bee.class, attractionZone);
            for (Bee bee : attractableBees) {
                if (!bee.hasNectar() && !bee.hasSavedFlowerPos() && !bee.isAngry()) {
                    bee.setSavedFlowerPos(this.blockPosition().above());
                }
            }

            // Pollinate bees right near the golem's sunflower
            AABB flowerZone = this.getBoundingBox().inflate(2.2D, 2.0D, 2.2D);
            List<Bee> nearbyBees = this.level().getEntitiesOfClass(Bee.class, flowerZone);
            for (Bee bee : nearbyBees) {
                if (!bee.hasNectar()) {
                    giveNectarToBee(bee);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FALLING_NECTAR,
                                bee.getX(), bee.getY() + 0.3D, bee.getZ(),
                                12, 0.2D, 0.2D, 0.2D, 0.05D);
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BEE_POLLINATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    break;
                }
            }
        }
    }

    private void giveNectarToBee(Bee bee) {
        bee.resetTicksWithoutNectarSinceExitingHive();
        if (BEE_DATA_FLAGS_ID != null) {
            byte currentFlags = bee.getEntityData().get(BEE_DATA_FLAGS_ID);
            bee.getEntityData().set(BEE_DATA_FLAGS_ID, (byte) (currentFlags | 8)); // 8 = FLAG_HAS_NECTAR
        } else {
            try {
                java.lang.reflect.Method setNectarMethod = Bee.class.getDeclaredMethod("setHasNectar", boolean.class);
                setNectarMethod.setAccessible(true);
                setNectarMethod.invoke(bee, true);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Extract honey with empty glass bottle
        if (itemstack.is(Items.GLASS_BOTTLE) && getHoneyLevel() > 0) {
            this.setHoneyLevel(getHoneyLevel() - 1);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LANDING_HONEY,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        12, 0.2D, 0.2D, 0.2D, 0.05D);
            }

            ItemStack honeyBottle = new ItemStack(Items.HONEY_BOTTLE);
            ItemStack result = ItemUtils.createFilledResult(itemstack, player, honeyBottle);
            player.setItemInHand(hand, result);

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("HoneyLevel", com.mojang.serialization.Codec.INT, getHoneyLevel());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("HoneyLevel", com.mojang.serialization.Codec.INT).ifPresent(this::setHoneyLevel);
    }

    @Override
    public boolean shouldKeepItem(ItemStack stack, int slotIndex) {
        return stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE) || stack.is(net.minecraft.world.item.Items.SHEARS);
    }
}
