package com.trycraft.trycraftmod.registry;

import com.trycraft.trycraftmod.TryCraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TryCraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TryCraft.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}