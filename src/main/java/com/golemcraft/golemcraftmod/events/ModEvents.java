package com.golemcraft.golemcraftmod.events;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = GolemCraft.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);

            // Se la struttura è un Iron Golem vanilla (T-shape di ferro), lascia fare a Minecraft vanilla!
            if (isIronGolemStructure(level, pos)) {
                return;
            }

            Integer oxidation = null;
            boolean waxed = false;

            if (belowState.is(Blocks.IRON_BLOCK)) {
                oxidation = 0;
                waxed = false;
            } else {
                oxidation = getOxidationFromBlock(belowState);
                if (oxidation != null) {
                    waxed = isWaxedCopperBlock(belowState);
                }
            }

            if (oxidation != null) {
                // Destroy blocks
                level.destroyBlock(pos, false);
                level.destroyBlock(belowPos, false);

                // Spawn Base Golem
                com.golemcraft.golemcraftmod.entity.BaseGolemEntity golem = ModEntities.BASE_GOLEM.get().create(level, EntitySpawnReason.EVENT);
                if (golem != null) {
                    golem.setOxidationLevel(oxidation);
                    golem.setWaxed(waxed);
                    if (event.getEntity() instanceof Player player) {
                        golem.setOwnerUUID(player.getUUID());
                        golem.setYRot(player.getYRot() + 180.0F);
                        golem.yBodyRot = golem.getYRot();
                        golem.yHeadRot = golem.getYRot();
                    }
                    golem.setPos(pos.getX() + 0.5D, belowPos.getY(), pos.getZ() + 0.5D);
                    level.addFreshEntity(golem);
                }
            }
        }
    }

    private static boolean isIronGolemStructure(Level level, BlockPos pumpkinPos) {
        BlockPos center = pumpkinPos.below();
        if (!level.getBlockState(center).is(Blocks.IRON_BLOCK)) {
            return false;
        }
        BlockPos bottom = center.below();
        if (!level.getBlockState(bottom).is(Blocks.IRON_BLOCK)) {
            return false;
        }
        boolean xArms = level.getBlockState(center.east()).is(Blocks.IRON_BLOCK) 
                     && level.getBlockState(center.west()).is(Blocks.IRON_BLOCK);
        boolean zArms = level.getBlockState(center.north()).is(Blocks.IRON_BLOCK) 
                     && level.getBlockState(center.south()).is(Blocks.IRON_BLOCK);
        return xArms || zArms;
    }

    private static Integer getOxidationFromBlock(BlockState state) {
        if (state.getBlock() instanceof WeatheringCopper copper) {
            return copper.getAge().ordinal();
        }
        Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock());
        if (unwaxed instanceof WeatheringCopper copper) {
            return copper.getAge().ordinal();
        }
        return null;
    }

    private static boolean isWaxedCopperBlock(BlockState state) {
        return HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(state.getBlock());
    }
}
