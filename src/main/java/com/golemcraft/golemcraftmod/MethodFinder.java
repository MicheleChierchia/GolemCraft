package com.golemcraft.golemcraftmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public class MethodFinder {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : Mob.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("save") || m.getName().toLowerCase().contains("read")) {
                System.out.println("Mob method: " + m);
            }
        }
        for (java.lang.reflect.Method m : Entity.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("save") || m.getName().toLowerCase().contains("read")) {
                System.out.println("Entity method: " + m);
            }
        }
    }
}
