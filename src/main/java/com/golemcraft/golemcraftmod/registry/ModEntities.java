package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.FlowerGolemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, GolemCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FlowerGolemEntity>> FLOWER_GOLEM =
            ENTITY_TYPES.register("flower_golem", () ->
                    EntityType.Builder.of(FlowerGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "flower_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.golemcraft.golemcraftmod.entity.BaseGolemEntity>> BASE_GOLEM =
            ENTITY_TYPES.register("base_golem", () ->
                    EntityType.Builder.of(com.golemcraft.golemcraftmod.entity.BaseGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "base_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.golemcraft.golemcraftmod.entity.FarmerGolemEntity>> FARMER_GOLEM =
            ENTITY_TYPES.register("farmer_golem", () ->
                    EntityType.Builder.of(com.golemcraft.golemcraftmod.entity.FarmerGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "farmer_golem"))));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
