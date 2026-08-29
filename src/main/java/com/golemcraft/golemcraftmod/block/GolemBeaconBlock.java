package com.golemcraft.golemcraftmod.block;

import com.golemcraft.golemcraftmod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class GolemBeaconBlock extends BaseEntityBlock {

    public static final MapCodec<GolemBeaconBlock> CODEC = simpleCodec(GolemBeaconBlock::new);

    public static final IntegerProperty OXIDATION = IntegerProperty.create("oxidation", 0, 3);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty WAXED = BooleanProperty.create("waxed");

    private static final float OXIDATION_CHANCE = 0.75f;

    public GolemBeaconBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OXIDATION, 0)
                .setValue(POWERED, false)
                .setValue(WAXED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OXIDATION, POWERED, WAXED);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(WAXED) && state.getValue(OXIDATION) < 3;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(WAXED) || state.getValue(OXIDATION) >= 3) return;
        if (random.nextFloat() < OXIDATION_CHANCE) {
            level.setBlock(pos, state.setValue(OXIDATION, state.getValue(OXIDATION) + 1), Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.is(Items.HONEYCOMB) && !state.getValue(WAXED)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WAXED, true), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (stack.getItem() instanceof AxeItem) {
            if (state.getValue(WAXED)) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(WAXED, false), Block.UPDATE_ALL);
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
            int oxidation = state.getValue(OXIDATION);
            if (oxidation > 0) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(OXIDATION, oxidation - 1), Block.UPDATE_ALL);
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof GolemBeaconBlockEntity be) {
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GolemBeaconBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.GOLEM_BEACON.get(), GolemBeaconBlockEntity::tick);
    }
}
