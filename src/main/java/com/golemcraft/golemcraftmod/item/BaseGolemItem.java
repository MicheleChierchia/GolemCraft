package com.golemcraft.golemcraftmod.item;

import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;

public class BaseGolemItem extends Item {
    public BaseGolemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        
        ItemStack itemstack = context.getItemInHand();
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos spawnPos = blockpos.relative(direction);
        
        com.golemcraft.golemcraftmod.entity.BaseGolemEntity golem = ModEntities.BASE_GOLEM.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (golem != null) {
            golem.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
            Player player = context.getPlayer();
            if (player != null) {
                golem.setOwnerUUID(player.getUUID());
                golem.setYRot(player.getYRot() + 180.0F); // Face the player
                golem.yBodyRot = golem.getYRot();
                golem.yHeadRot = golem.getYRot();
            }
            serverLevel.addFreshEntity(golem);
            if (player != null && !player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }
}
