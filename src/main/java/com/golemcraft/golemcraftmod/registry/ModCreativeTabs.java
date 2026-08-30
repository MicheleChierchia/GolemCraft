package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GolemCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOLEMCRAFT_TAB = CREATIVE_MODE_TABS.register("golemcraft_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.golemcraft"))
                    .icon(() -> new ItemStack(ModBlocks.BASE_GOLEM_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.GOLEM_MANUAL_ITEM.get());
                        output.accept(ModBlocks.BASE_GOLEM_ITEM.get());
                        output.accept(ModBlocks.GOLEM_BEACON_ITEM.get());
                        output.accept(ModBlocks.GOLEM_COMPASS_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
