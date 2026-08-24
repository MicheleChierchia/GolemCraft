package com.golemcraft.golemcraftmod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.List;
import java.util.function.Consumer;

public class GolemFakePlayerHelper {
    
    public static void executeAsPlayer(BaseGolemEntity golem, Consumer<FakePlayer> action) {
        if (!(golem.level() instanceof ServerLevel sl)) return;
        
        FakePlayer player = FakePlayerFactory.getMinecraft(sl);
        player.setPos(golem.getX(), golem.getY(), golem.getZ());
        player.setYRot(golem.getYRot());
        player.setXRot(golem.getXRot());
        player.yHeadRot = golem.yHeadRot;
        player.yBodyRot = golem.yBodyRot;
        
        player.getInventory().clearContent();
        
        player.setItemInHand(InteractionHand.MAIN_HAND, golem.getItemInHand(InteractionHand.MAIN_HAND).copy());
        player.setItemInHand(InteractionHand.OFF_HAND, golem.getItemInHand(InteractionHand.OFF_HAND).copy());
        
        for (int i = 0; i < golem.getInventory().getContainerSize(); i++) {
            if (i + 1 < 36) {
                player.getInventory().setItem(i + 1, golem.getInventory().getItem(i).copy());
            }
        }
        
        List<Entity> before = sl.getEntities(player, player.getBoundingBox().inflate(10.0D), e -> e instanceof Projectile && ((Projectile)e).getOwner() == player);
        
        action.accept(player);
        
        List<Entity> after = sl.getEntities(player, player.getBoundingBox().inflate(10.0D), e -> e instanceof Projectile && ((Projectile)e).getOwner() == player);
        for (Entity e : after) {
            if (!before.contains(e)) {
                ((Projectile)e).setOwner(golem);
            }
        }
        
        golem.setItemInHand(InteractionHand.MAIN_HAND, player.getItemInHand(InteractionHand.MAIN_HAND));
        golem.setItemInHand(InteractionHand.OFF_HAND, player.getItemInHand(InteractionHand.OFF_HAND));
        
        for (int i = 0; i < golem.getInventory().getContainerSize(); i++) {
            if (i + 1 < 36) {
                golem.getInventory().setItem(i, player.getInventory().getItem(i + 1));
            }
        }
    }
}
