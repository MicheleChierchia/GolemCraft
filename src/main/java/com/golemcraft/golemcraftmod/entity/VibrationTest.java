package com.golemcraft.golemcraftmod.entity;

import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class VibrationTest extends Mob implements VibrationSystem, VibrationSystem.User {

    private final VibrationSystem.Data vibrationData;

    public VibrationTest(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.vibrationData = new VibrationSystem.Data();
    }

    @Override
    public VibrationSystem.Data getVibrationData() { return this.vibrationData; }

    @Override
    public VibrationSystem.User getVibrationUser() { return this; }

    @Override
    public void onReceiveVibration(ServerLevel p_281476_, BlockPos p_281222_, Holder<GameEvent> p_316223_, Entity p_283525_, Entity p_281313_, float p_281254_) {}
    
    @Override
    public boolean canReceiveVibration(ServerLevel p_281213_, BlockPos p_281373_, Holder<GameEvent> p_316664_, GameEvent.Context p_281363_) { return true; }

    @Override
    public PositionSource getPositionSource() { return new EntityPositionSource(this, this.getEyeHeight()); }

    @Override
    public int getListenerRadius() { return 16; }
}
