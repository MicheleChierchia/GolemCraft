package com.golemcraft.golemcraftmod.entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import java.util.function.BiConsumer;

public abstract class ListenerTest extends Mob implements VibrationSystem {
    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener;
    
    protected ListenerTest() { 
        super(null, null); 
        this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    }
    
    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, net.minecraft.server.level.ServerLevel> p_218348_) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverlevel) {
            p_218348_.accept(this.dynamicGameEventListener, serverlevel);
        }
    }
}
