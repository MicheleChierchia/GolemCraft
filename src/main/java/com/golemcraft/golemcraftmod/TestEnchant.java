package com.golemcraft.golemcraftmod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;

public class TestEnchant {
    public static int getLure(Level level, ItemStack stack) {
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var lure = registry.getOrThrow(Enchantments.LURE);
        return stack.getEnchantmentLevel(lure);
    }
}
