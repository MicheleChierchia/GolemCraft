package com.golemcraft.golemcraftmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class GolemManualItem extends Item {
    public GolemManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            com.golemcraft.golemcraftmod.GolemCraftClient.openManualScreen();
        }
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipOutput, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipOutput, flag);
        tooltipOutput.accept(Component.translatable("item.golemcraft.golem_manual.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
