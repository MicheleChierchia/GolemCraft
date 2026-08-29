package com.golemcraft.golemcraftmod.item;

import com.golemcraft.golemcraftmod.entity.ExplorerGolemEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class GolemCompassItem extends Item {

    /** NBT key stored in CUSTOM_DATA component to identify the linked golem */
    public static final String GOLEM_UUID_KEY = "GolemUUID";

    public GolemCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel serverLevel, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, serverLevel, entity, slot);

        if (!(entity instanceof Player player)) return;

        // Throttle updates to every 10 ticks (0.5s) per player
        if (player.tickCount % 10 != 0) return;

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;

        CompoundTag tag = data.copyTag();
        tag.getString(GOLEM_UUID_KEY).ifPresent(uuidStr -> {
            try {
                UUID golemUUID = UUID.fromString(uuidStr);
                Entity targetEntity = serverLevel.getEntity(golemUUID);
                if (targetEntity instanceof ExplorerGolemEntity explorer && explorer.isAlive()) {
                    GlobalPos currentPos = GlobalPos.of(explorer.level().dimension(), explorer.blockPosition());
                    LodestoneTracker currentTracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    if (currentTracker == null || currentTracker.target().isEmpty() || !currentTracker.target().get().equals(currentPos)) {
                        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(currentPos), false));
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipOutput, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipOutput, flag);
        tooltipOutput.accept(Component.translatable("item.golemcraft.golem_compass.tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
