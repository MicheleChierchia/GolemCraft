package com.golemcraft.golemcraftmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class GolemCompassItem extends Item {

    /** NBT key stored in CUSTOM_DATA component to identify the linked golem */
    public static final String GOLEM_UUID_KEY = "GolemUUID";

    public GolemCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipOutput, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipOutput, flag);
        tooltipOutput.accept(Component.translatable("item.golemcraft.golem_compass.tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
