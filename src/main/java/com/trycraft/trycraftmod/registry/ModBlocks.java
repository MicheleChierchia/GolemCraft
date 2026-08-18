package com.trycraft.trycraftmod.registry;

import com.trycraft.trycraftmod.TryCraft;
import com.trycraft.trycraftmod.blocks.TryBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // 1. Inizializza i registri per Blocchi e Oggetti
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TryCraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TryCraft.MODID);

    // 2. Registra il tuo blocco
    // BlockBehaviour.Properties definisce attributi fisici base come resistenza o suoni
    public static final DeferredBlock<Block> MIO_BLOCCO = BLOCKS.register("tryblock",
            () -> new TryBlock(BlockBehaviour.Properties.of().strength(3.0f, 3.0f)));

    // 3. Registra l'oggetto associato al blocco (BlockItem)
    public static final DeferredItem<Item> MIO_BLOCCO_ITEM = ITEMS.register("try_block",
            () -> new BlockItem(MIO_BLOCCO.get(), new Item.Properties()));

    // 4. Metodo per collegare i registri all'Event Bus
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}