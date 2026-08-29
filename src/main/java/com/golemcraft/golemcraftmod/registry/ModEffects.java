package com.golemcraft.golemcraftmod.registry;

import com.golemcraft.golemcraftmod.GolemCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, GolemCraft.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> CHARGE = MOB_EFFECTS.register("charge",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00) {});

    public static final DeferredHolder<MobEffect, MobEffect> OXIDATION_IMMUNITY = MOB_EFFECTS.register("oxidation_immunity",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x00FFFF) {});

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
