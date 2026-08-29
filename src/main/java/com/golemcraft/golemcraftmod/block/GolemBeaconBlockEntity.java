package com.golemcraft.golemcraftmod.block;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import com.golemcraft.golemcraftmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.SimpleContainer;

import javax.annotation.Nullable;
import java.util.*;

public class GolemBeaconBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

    final SimpleContainerData containerData = new SimpleContainerData(5);

    private int tier1Effect = 0;
    private int tier2Effect = 0;
    private int tier3Effect = 0;
    private int secondaryEffect = 0;
    private int levels = 0;

    private final SimpleContainer paymentSlot = new SimpleContainer(1);

    private final Set<UUID> chargedGolemUUIDs = new HashSet<>();
    private int tickCount = 0;

    private static final Set<Block> VALID_COPPER_BLOCKS;

    static {
        Set<Block> blocks = new HashSet<>();
        WeatheringCopperCollection<Block> collection = Blocks.COPPER_BLOCK;
        collection.weathering().forEach(blocks::add);
        collection.waxed().forEach(blocks::add);
        VALID_COPPER_BLOCKS = Collections.unmodifiableSet(blocks);
    }

    public GolemBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLEM_BEACON.get(), pos, state);
    }

    public void setEffects(int t1, int t2, int t3, int sec) {
        this.tier1Effect = t1;
        this.tier2Effect = t2;
        this.tier3Effect = t3;
        this.secondaryEffect = sec;
        this.setChanged();
    }

    public SimpleContainer getPaymentSlot() { return paymentSlot; }

    public int getTier1Effect() { return tier1Effect; }
    public int getTier2Effect() { return tier2Effect; }
    public int getTier3Effect() { return tier3Effect; }
    public int getSecondaryEffect() { return secondaryEffect; }
    public int getLevels() { return levels; }

    public static void tick(Level level, BlockPos pos, BlockState state, GolemBeaconBlockEntity be) {
        if (level.isClientSide()) return;

        be.tickCount++;
        if (be.tickCount % 40 == 0) {
            be.updateBeacon((ServerLevel) level, pos, state, be);
        }

        be.containerData.set(0, be.tier1Effect);
        be.containerData.set(1, be.tier2Effect);
        be.containerData.set(2, be.tier3Effect);
        be.containerData.set(3, be.secondaryEffect);
        be.containerData.set(4, be.levels);
    }

    private void updateBeacon(ServerLevel serverLevel, BlockPos pos, BlockState state, GolemBeaconBlockEntity be) {
        int tier = checkPyramid(serverLevel);
        be.levels = tier;

        if (tier > 0) {
            applyEffects(serverLevel, tier);
        }
        manageChargedState(serverLevel, tier);

        boolean powered = tier > 0;
        if (state.getValue(GolemBeaconBlock.POWERED) != powered) {
            serverLevel.setBlock(pos, state.setValue(GolemBeaconBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private int checkPyramid(ServerLevel level) {
        if (!level.canSeeSky(worldPosition.above())) return 0;

        int tier = 0;
        for (int layer = 1; layer <= 4; layer++) {
            int y = worldPosition.getY() - layer;
            boolean layerComplete = true;
            for (int x = -layer; x <= layer && layerComplete; x++) {
                for (int z = -layer; z <= layer && layerComplete; z++) {
                    BlockPos checkPos = new BlockPos(worldPosition.getX() + x, y, worldPosition.getZ() + z);
                    if (!VALID_COPPER_BLOCKS.contains(level.getBlockState(checkPos).getBlock())) {
                        layerComplete = false;
                    }
                }
            }
            if (layerComplete) {
                tier = layer;
            } else {
                break;
            }
        }
        return tier;
    }

    private void applyEffects(ServerLevel level, int tier) {
        double range = 10 + (tier * 10.0);
        int duration = (9 + (tier * 2)) * 20;

        AABB area = new AABB(worldPosition).inflate(range);
        List<BaseGolemEntity> golems = level.getEntitiesOfClass(BaseGolemEntity.class, area);

        applySpecificEffect(tier1Effect, tier >= 1, 0, duration, golems);
        applySpecificEffect(tier2Effect, tier >= 2, 0, duration, golems);
        applySpecificEffect(tier3Effect, tier >= 3, 0, duration, golems);
        
        if (tier >= 4 && secondaryEffect != 0) {
            int amp = 1;
            if (secondaryEffect == tier1Effect || secondaryEffect == tier2Effect || secondaryEffect == tier3Effect) {
                applySpecificEffect(secondaryEffect, true, 1, duration, golems);
            } else {
                applySpecificEffect(secondaryEffect, true, 0, duration, golems);
            }
        }
    }

    private void applySpecificEffect(int id, boolean allowed, int amplifier, int duration, List<BaseGolemEntity> golems) {
        if (!allowed || id == 0) return;
        MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.byId(id);
        if (effect == null) return;
        Holder<MobEffect> holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        for (BaseGolemEntity golem : golems) {
            golem.addEffect(new MobEffectInstance(holder, duration, amplifier, true, true));
        }
    }

    private void manageChargedState(ServerLevel level, int tier) {
        if (tier < 3 || tier3Effect == 0) {
            if (!chargedGolemUUIDs.isEmpty()) {
                for (UUID uuid : chargedGolemUUIDs) {
                    Entity entity = level.getEntity(uuid);
                    if (entity instanceof BaseGolemEntity golem) {
                        golem.setCharged(false);
                    }
                }
                chargedGolemUUIDs.clear();
                setChanged();
            }
            return;
        }

        Holder<MobEffect> chargeEffect = com.golemcraft.golemcraftmod.registry.ModEffects.CHARGE.getDelegate();
        int chargeId = chargeEffect != null ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(chargeEffect.value()) : 0;
        
        if (tier3Effect == chargeId) {
            double range = 10 + (tier * 10.0);
            AABB area = new AABB(worldPosition).inflate(range);
            List<BaseGolemEntity> golems = level.getEntitiesOfClass(BaseGolemEntity.class, area);
            for (BaseGolemEntity golem : golems) {
                if (!golem.isCharged()) {
                    golem.setCharged(true);
                    chargedGolemUUIDs.add(golem.getUUID());
                    setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Tier1", com.mojang.serialization.Codec.INT, tier1Effect);
        output.store("Tier2", com.mojang.serialization.Codec.INT, tier2Effect);
        output.store("Tier3", com.mojang.serialization.Codec.INT, tier3Effect);
        output.store("Secondary", com.mojang.serialization.Codec.INT, secondaryEffect);
        output.store("ChargedGolems", com.mojang.serialization.Codec.list(net.minecraft.core.UUIDUtil.CODEC), new ArrayList<>(chargedGolemUUIDs));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("Tier1", com.mojang.serialization.Codec.INT).ifPresent(v -> tier1Effect = v);
        input.read("Tier2", com.mojang.serialization.Codec.INT).ifPresent(v -> tier2Effect = v);
        input.read("Tier3", com.mojang.serialization.Codec.INT).ifPresent(v -> tier3Effect = v);
        input.read("Secondary", com.mojang.serialization.Codec.INT).ifPresent(v -> secondaryEffect = v);
        input.read("ChargedGolems", com.mojang.serialization.Codec.list(net.minecraft.core.UUIDUtil.CODEC)).ifPresent(list -> {
            chargedGolemUUIDs.clear();
            chargedGolemUUIDs.addAll(list);
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.golemcraft.golem_beacon");
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player player) {
        return new GolemBeaconMenu(id, inv, this);
    }
}
