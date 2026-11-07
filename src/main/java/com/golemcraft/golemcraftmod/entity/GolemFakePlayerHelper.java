package com.golemcraft.golemcraftmod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.function.Consumer;

public class GolemFakePlayerHelper {
    
    public static void executeAsPlayer(BaseGolemEntity golem, Consumer<FakePlayer> action) {
        if (!(golem.level() instanceof ServerLevel sl)) return;
        
        FakePlayer player = FakePlayerFactory.getMinecraft(sl);
        player.setPos(golem.getX(), golem.getEyeY() - player.getEyeHeight(), golem.getZ());
        player.setYRot(golem.getYRot());
        player.setXRot(golem.getXRot());
        player.yHeadRot = golem.yHeadRot;
        player.yBodyRot = golem.yBodyRot;
        
        player.setItemInHand(InteractionHand.MAIN_HAND, golem.getItemInHand(InteractionHand.MAIN_HAND).copy());
        player.setItemInHand(InteractionHand.OFF_HAND, golem.getItemInHand(InteractionHand.OFF_HAND).copy());
        
        action.accept(player);
        
        golem.setItemInHand(InteractionHand.MAIN_HAND, player.getItemInHand(InteractionHand.MAIN_HAND));
        golem.setItemInHand(InteractionHand.OFF_HAND, player.getItemInHand(InteractionHand.OFF_HAND));
        
        player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
    }
}
