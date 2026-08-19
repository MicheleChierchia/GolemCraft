package com.golemcraft.golemcraftmod.events;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.FlowerGolemEntity;
import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = GolemCraft.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.is(Blocks.CARVED_PUMPKIN)) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);

            if (belowState.is(Blocks.IRON_BLOCK)) {
                // Destroy blocks
                level.destroyBlock(pos, false);
                level.destroyBlock(belowPos, false);

                // Spawn Base Golem
                com.golemcraft.golemcraftmod.entity.BaseGolemEntity golem = ModEntities.BASE_GOLEM.get().create(level, EntitySpawnReason.EVENT);
                if (golem != null) {
                    golem.setPos(pos.getX() + 0.5D, belowPos.getY(), pos.getZ() + 0.5D);
                    level.addFreshEntity(golem);
                }
            }
        }
    }
}
