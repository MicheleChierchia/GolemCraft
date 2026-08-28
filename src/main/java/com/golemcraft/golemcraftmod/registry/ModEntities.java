package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.FlowerGolemEntity;
import com.golemcraft.golemcraftmod.entity.FishermanGolemEntity;
import com.golemcraft.golemcraftmod.entity.SoldierGolemEntity;
import com.golemcraft.golemcraftmod.entity.DepthGolemEntity;
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

    public static final DeferredHolder<EntityType<?>, EntityType<FishermanGolemEntity>> FISHERMAN_GOLEM =
            ENTITY_TYPES.register("fisherman_golem", () ->
                    EntityType.Builder.of(FishermanGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "fisherman_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SoldierGolemEntity>> SOLDIER_GOLEM =
            ENTITY_TYPES.register("soldier_golem", () ->
                    EntityType.Builder.of(SoldierGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "soldier_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.golemcraft.golemcraftmod.entity.LumberjackGolemEntity>> LUMBERJACK_GOLEM =
            ENTITY_TYPES.register("lumberjack_golem", () ->
                    EntityType.Builder.of(com.golemcraft.golemcraftmod.entity.LumberjackGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "lumberjack_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<DepthGolemEntity>> DEPTH_GOLEM =
            ENTITY_TYPES.register("depth_golem", () ->
                    EntityType.Builder.of(DepthGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "depth_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.golemcraft.golemcraftmod.entity.ExplorerGolemEntity>> EXPLORER_GOLEM =
            ENTITY_TYPES.register("explorer_golem", () ->
                    EntityType.Builder.of(com.golemcraft.golemcraftmod.entity.ExplorerGolemEntity::new, MobCategory.MISC)
                            .sized(0.5f, 1.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "explorer_golem"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.golemcraft.golemcraftmod.entity.projectile.SonicBoomProjectile>> SONIC_BOOM_PROJECTILE =
            ENTITY_TYPES.register("sonic_boom_projectile", () ->
                    EntityType.Builder.<com.golemcraft.golemcraftmod.entity.projectile.SonicBoomProjectile>of(com.golemcraft.golemcraftmod.entity.projectile.SonicBoomProjectile::new, MobCategory.MISC)
                            .noSave()
                            .noSummon()
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "sonic_boom_projectile"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.golemcraft.golemcraftmod.entity.projectile.GolemFishingHook>> GOLEM_FISHING_HOOK =
            ENTITY_TYPES.register("golem_fishing_hook", () ->
                    EntityType.Builder.<com.golemcraft.golemcraftmod.entity.projectile.GolemFishingHook>of(com.golemcraft.golemcraftmod.entity.projectile.GolemFishingHook::new, MobCategory.MISC)
                            .noSave()
                            .noSummon()
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(5)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GolemCraft.MODID, "golem_fishing_hook"))));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
