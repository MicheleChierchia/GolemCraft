package com.golemcraft.golemcraftmod.block;

import com.golemcraft.golemcraftmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class AreaMarkerBlockEntity extends BlockEntity {

    private BlockPos partnerPos = null;
    private int checkPartnerTimer = 0;

    public AreaMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AREA_MARKER.get(), pos, state);
    }

    public BlockPos getPartnerPos() {
        return partnerPos;
    }

    public void setPartnerPos(BlockPos pos) {
        this.partnerPos = pos;
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            boolean shouldBeLit = pos != null;
            if (state.hasProperty(AreaMarkerBlock.LIT) && state.getValue(AreaMarkerBlock.LIT) != shouldBeLit) {
                serverLevel.setBlock(this.worldPosition, state.setValue(AreaMarkerBlock.LIT, shouldBeLit), Block.UPDATE_ALL);
            }
            serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public boolean hasPartner() {
        return this.partnerPos != null;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AreaMarkerBlockEntity be) {
        if (!be.hasPartner()) return;

        BlockPos partner = be.getPartnerPos();

        // 1. Luce e particelle sulla testa del marker attivo
        if (level.getRandom().nextInt(2) == 0) {
            level.addParticle(new DustParticleOptions(0x33FF66, 1.0F),
                    pos.getX() + 0.5D, pos.getY() + 0.72D, pos.getZ() + 0.5D,
                    0.0D, 0.015D, 0.0D);
        }
        if (level.getRandom().nextInt(5) == 0) {
            level.addParticle(ParticleTypes.GLOW,
                    pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D,
                    (level.getRandom().nextDouble() - 0.5D) * 0.02D, 0.025D, (level.getRandom().nextDouble() - 0.5D) * 0.02D);
        }

        // 2. Delimitazione perimetrale: disegna il perimetro rettangolare continuo
        // Solo il marker con coordinate inferiori calcola il perimetro per evitare duplicati
        if (pos.compareTo(partner) > 0) return;

        double minX = Math.min(pos.getX(), partner.getX()) + 0.5D;
        double maxX = Math.max(pos.getX(), partner.getX()) + 0.5D;
        double minZ = Math.min(pos.getZ(), partner.getZ()) + 0.5D;
        double maxZ = Math.max(pos.getZ(), partner.getZ()) + 0.5D;
        double y = Math.min(pos.getY(), partner.getY()) + 0.72D;

        double widthX = maxX - minX;
        double widthZ = maxZ - minZ;

        DustParticleOptions perimeterDust = new DustParticleOptions(0x22EE66, 1.2F);

        // Spawna particelle verdi luminose lungo i 4 bordi dell'area partendo dai marker
        int particlesPerEdge = Math.max(4, Math.min(24, (int) ((widthX + widthZ) * 0.75D)));
        for (int i = 0; i < particlesPerEdge; i++) {
            double tX = level.getRandom().nextDouble();
            double tZ = level.getRandom().nextDouble();

            // Bordi Nord e Sud (collegano esattamente le coordinate X dei marker)
            level.addParticle(perimeterDust, minX + tX * widthX, y, minZ, 0.0D, 0.005D, 0.0D);
            level.addParticle(perimeterDust, minX + tX * widthX, y, maxZ, 0.0D, 0.005D, 0.0D);

            // Bordi Ovest ed Est (collegano esattamente le coordinate Z dei marker)
            level.addParticle(perimeterDust, minX, y, minZ + tZ * widthZ, 0.0D, 0.005D, 0.0D);
            level.addParticle(perimeterDust, maxX, y, minZ + tZ * widthZ, 0.0D, 0.005D, 0.0D);
        }

        // Bagliori evidenti sui 4 angoli dell'area perimetrata (inclusi i due marker)
        if (level.getRandom().nextInt(4) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, minX, y + 0.1D, minZ, 0.0D, 0.02D, 0.0D);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, maxX, y + 0.1D, minZ, 0.0D, 0.02D, 0.0D);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, minX, y + 0.1D, maxZ, 0.0D, 0.02D, 0.0D);
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, maxX, y + 0.1D, maxZ, 0.0D, 0.02D, 0.0D);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AreaMarkerBlockEntity be) {
        if (!be.hasPartner()) return;

        be.checkPartnerTimer++;
        if (be.checkPartnerTimer >= 40) {
            be.checkPartnerTimer = 0;
            if (level.isLoaded(be.partnerPos)) {
                if (!(level.getBlockEntity(be.partnerPos) instanceof AreaMarkerBlockEntity partnerBe) ||
                    (partnerBe.getPartnerPos() != null && !partnerBe.getPartnerPos().equals(pos))) {
                    be.setPartnerPos(null);
                }
            }
        }
    }

    public AABB getWorkBounds() {
        if (!hasPartner()) return null;
        int minX = Math.min(worldPosition.getX(), partnerPos.getX());
        int maxX = Math.max(worldPosition.getX(), partnerPos.getX());
        int minZ = Math.min(worldPosition.getZ(), partnerPos.getZ());
        int maxZ = Math.max(worldPosition.getZ(), partnerPos.getZ());
        return new AABB(minX, worldPosition.getY(), minZ, maxX, worldPosition.getY(), maxZ);
    }

    public int getTargetY() {
        if (partnerPos != null) {
            return Math.min(worldPosition.getY(), partnerPos.getY());
        }
        return worldPosition.getY();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.partnerPos != null) {
            output.store("PartnerPos", BlockPos.CODEC, this.partnerPos);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("PartnerPos", BlockPos.CODEC).ifPresent(pos -> this.partnerPos = pos);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
