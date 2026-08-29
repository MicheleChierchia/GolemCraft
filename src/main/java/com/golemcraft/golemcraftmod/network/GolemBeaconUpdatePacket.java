package com.golemcraft.golemcraftmod.network;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.block.GolemBeaconBlockEntity;
import com.golemcraft.golemcraftmod.block.GolemBeaconMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GolemBeaconUpdatePacket(BlockPos pos, int t1, int t2, int t3, int sec) implements CustomPacketPayload {

    public static final Type<GolemBeaconUpdatePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GolemCraft.MODID, "golem_beacon_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GolemBeaconUpdatePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.t1);
                buf.writeInt(packet.t2);
                buf.writeInt(packet.t3);
                buf.writeInt(packet.sec);
            },
            buf -> new GolemBeaconUpdatePacket(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GolemBeaconUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.isLoaded(packet.pos) && level.getBlockEntity(packet.pos) instanceof GolemBeaconBlockEntity be) {
                    // Controlla se il menu è aperto o se l'utente ha inserito qualcosa (per sicurezza)
                    if (player.containerMenu instanceof GolemBeaconMenu menu) {
                        if (!menu.getSlot(0).getItem().isEmpty()) {
                            menu.getSlot(0).remove(1);
                        }
                    }
                    // Ma SALVA GLI EFFETTI a prescindere per prevenire il bug di desync (nel caso il menu si fosse già chiuso sul server)
                    be.setEffects(packet.t1, packet.t2, packet.t3, packet.sec);
                }
            }
        });
    }
}
