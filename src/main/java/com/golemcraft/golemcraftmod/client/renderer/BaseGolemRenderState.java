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
    public boolean isFishing;
    public boolean isDepthGolem;
    /** Used by ExplorerGolemRenderer: golem is in dormant/waiting state */
    public boolean isWaiting;
    public boolean isExplorerGolem;
    public boolean isCharged;
    public int honeyLevel;
    public boolean isBeekeeperGolem;
    public int alchemistPotionAction;
    public final net.minecraft.client.renderer.item.ItemStackRenderState alchemistPotionItemState =
            new net.minecraft.client.renderer.item.ItemStackRenderState();
    public net.minecraft.world.phys.Vec3 laserOriginPosition;
    public net.minecraft.world.phys.Vec3 laserTargetPosition;
    public float laserAttackTime;
    public float laserAttackScale;
    public boolean isSwimming;
    public float swimAmount;
}
