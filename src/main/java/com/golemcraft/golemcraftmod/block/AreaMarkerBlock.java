package com.golemcraft.golemcraftmod.block;

import com.golemcraft.golemcraftmod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class AreaMarkerBlock extends BaseEntityBlock {

    public static final MapCodec<AreaMarkerBlock> CODEC = simpleCodec(AreaMarkerBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 11.0D, 10.0D);

    public AreaMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AreaMarkerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.AREA_MARKER.get(), AreaMarkerBlockEntity::clientTick);
        } else {
            return createTickerHelper(type, ModBlockEntities.AREA_MARKER.get(), AreaMarkerBlockEntity::serverTick);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AreaMarkerBlockEntity thisMarker)) return;

        // Cerca il marker non accoppiato più vicino entro un raggio di 32 blocchi orizzontali e 16 verticali
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-32, -16, -32), pos.offset(32, 16, 32))) {
            if (checkPos.equals(pos)) continue;
            if (level.getBlockEntity(checkPos) instanceof AreaMarkerBlockEntity otherMarker) {
                if (!otherMarker.hasPartner()) {
                    double distSq = pos.distSqr(checkPos);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = checkPos.immutable();
                    }
                }
            }
        }

        if (nearest != null && level.getBlockEntity(nearest) instanceof AreaMarkerBlockEntity otherMarker) {
            thisMarker.setPartnerPos(nearest);
            otherMarker.setPartnerPos(pos);

            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.8F);
            level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.05D);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, nearest.getX() + 0.5D, nearest.getY() + 0.7D, nearest.getZ() + 0.5D, 15, 0.2D, 0.2D, 0.2D, 0.05D);
                sl.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, 10, 0.1D, 0.1D, 0.1D, 0.02D);
                sl.sendParticles(ParticleTypes.GLOW, nearest.getX() + 0.5D, nearest.getY() + 0.7D, nearest.getZ() + 0.5D, 10, 0.1D, 0.1D, 0.1D, 0.02D);
            }

            if (placer instanceof Player player) {
                int dx = Math.abs(pos.getX() - nearest.getX()) + 1;
                int dz = Math.abs(pos.getZ() - nearest.getZ()) + 1;
                int targetY = Math.min(pos.getY(), nearest.getY());
                player.sendOverlayMessage(Component.translatable("message.golemcraft.marker_linked", dx, dz, targetY));
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof AreaMarkerBlockEntity marker) {
                if (marker.hasPartner()) {
                    BlockPos partner = marker.getPartnerPos();
                    int dx = Math.abs(pos.getX() - partner.getX()) + 1;
                    int dz = Math.abs(pos.getZ() - partner.getZ()) + 1;
                    int targetY = marker.getTargetY();
                    player.sendOverlayMessage(Component.translatable("message.golemcraft.marker_info_linked", dx, dz, targetY));
                } else {
                    player.sendOverlayMessage(Component.translatable("message.golemcraft.marker_info_unlinked"));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean willHarvest) {
        super.affectNeighborsAfterRemoval(state, level, pos, willHarvest);
        if (level.getBlockEntity(pos) instanceof AreaMarkerBlockEntity marker && marker.hasPartner()) {
            BlockPos partner = marker.getPartnerPos();
            if (level.isLoaded(partner) && level.getBlockEntity(partner) instanceof AreaMarkerBlockEntity partnerMarker) {
                partnerMarker.setPartnerPos(null);
            }
        }
    }
}
