package com.golemcraft.golemcraftmod;


import com.golemcraft.golemcraftmod.registry.ModBlocks;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GolemCraft.MODID)
public class GolemCraft {
    public static final String MODID = "golemcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GolemCraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModBlocks.register(modEventBus);
        com.golemcraft.golemcraftmod.registry.ModBlockEntities.register(modEventBus);
        com.golemcraft.golemcraftmod.registry.ModEffects.register(modEventBus);
        com.golemcraft.golemcraftmod.registry.ModEntities.register(modEventBus);
        com.golemcraft.golemcraftmod.registry.ModCreativeTabs.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("GolemCraft common setup initialized.");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(com.golemcraft.golemcraftmod.registry.ModBlocks.BASE_GOLEM_ITEM);
            event.accept(com.golemcraft.golemcraftmod.registry.ModBlocks.GOLEM_MANUAL_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(com.golemcraft.golemcraftmod.registry.ModBlocks.GOLEM_COMPASS_ITEM);
            event.accept(com.golemcraft.golemcraftmod.registry.ModBlocks.GOLEM_MANUAL_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(com.golemcraft.golemcraftmod.registry.ModBlocks.GOLEM_BEACON_ITEM);
        }
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

}
