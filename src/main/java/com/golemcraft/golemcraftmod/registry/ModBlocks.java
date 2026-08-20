package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GolemCraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GolemCraft.MODID);

    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> BASE_GOLEM_ITEM = ITEMS.registerItem("base_golem_item", 
            com.golemcraft.golemcraftmod.item.BaseGolemItem::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}