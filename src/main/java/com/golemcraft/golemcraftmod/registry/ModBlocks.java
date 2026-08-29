package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.block.GolemBeaconBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GolemCraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GolemCraft.MODID);

    public static final DeferredItem<Item> BASE_GOLEM_ITEM = ITEMS.registerItem("base_golem_item",
            com.golemcraft.golemcraftmod.item.BaseGolemItem::new);
    public static final DeferredItem<Item> FISHING_ROD_CAST_DUMMY = ITEMS.registerSimpleItem("fishing_rod_cast_dummy", Item.Properties::new);
    public static final DeferredItem<Item> GOLEM_COMPASS_ITEM = ITEMS.registerItem("golem_compass",
            com.golemcraft.golemcraftmod.item.GolemCompassItem::new);
    public static final DeferredItem<Item> GOLEM_MANUAL_ITEM = ITEMS.registerItem("golem_manual",
            com.golemcraft.golemcraftmod.item.GolemManualItem::new);

    public static final DeferredBlock<GolemBeaconBlock> GOLEM_BEACON = BLOCKS.registerBlock("golem_beacon",
            GolemBeaconBlock::new,
            props -> props.mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(SoundType.COPPER)
                    .lightLevel(state -> state.getValue(GolemBeaconBlock.POWERED) ? 15 : 0)
                    .randomTicks()
                    .noOcclusion());

    public static final DeferredItem<BlockItem> GOLEM_BEACON_ITEM = ITEMS.registerSimpleBlockItem(GOLEM_BEACON);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
