package com.golemcraft.golemcraftmod.client.renderer;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class BaseGolemRenderState extends ArmedEntityRenderState {
    public boolean isRummaging;
    public net.minecraft.world.entity.HumanoidArm mainArm;
    public int oxidationLevel;
    /** Used by SoldierGolemRenderer: 0.0 = idle, 1.0 = peak swing */
    public float attackAnimProgress;
    public boolean isAggressive;
    public boolean hasBow;
    public boolean isGuarding;
}
