package com.trycraft.trycraftmod.registry;

import com.trycraft.trycraftmod.TryCraft;
import com.trycraft.trycraftmod.entity.FlowerGolemEntity;
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
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, TryCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FlowerGolemEntity>> FLOWER_GOLEM =
            ENTITY_TYPES.register("flower_golem", () ->
                    EntityType.Builder.of(FlowerGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f) // Small size similar to copper golem
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(TryCraft.MODID, "flower_golem"))));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
