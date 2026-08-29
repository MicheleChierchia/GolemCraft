package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.block.GolemBeaconBlockEntity;
import com.golemcraft.golemcraftmod.block.GolemBeaconMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GolemCraft.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, GolemCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemBeaconBlockEntity>> GOLEM_BEACON =
            BLOCK_ENTITY_TYPES.register("golem_beacon", () ->
                    new BlockEntityType<>(GolemBeaconBlockEntity::new, ModBlocks.GOLEM_BEACON.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<GolemBeaconMenu>> GOLEM_BEACON_MENU =
            MENU_TYPES.register("golem_beacon", () ->
                    IMenuTypeExtension.create((windowId, inv, data) ->
                            new GolemBeaconMenu(windowId, inv, data)));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
        MENU_TYPES.register(eventBus);
    }
}
