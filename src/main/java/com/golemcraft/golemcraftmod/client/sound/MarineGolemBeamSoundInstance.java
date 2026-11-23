package com.golemcraft.golemcraftmod.client.sound;

import com.golemcraft.golemcraftmod.entity.MarineGolemEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class MarineGolemBeamSoundInstance extends AbstractTickableSoundInstance {
    private static final float PITCH_MIN = 0.7F;
    private static final float PITCH_SCALE = 0.5F;
    private final MarineGolemEntity golem;

    public MarineGolemBeamSoundInstance(MarineGolemEntity golem) {
        super(SoundEvents.GUARDIAN_ATTACK, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.golem = golem;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
    }

    public static void playFor(MarineGolemEntity golem) {
        Minecraft.getInstance().getSoundManager().play(new MarineGolemBeamSoundInstance(golem));
    }

    @Override
    public boolean canPlaySound() {
        return !this.golem.isSilent();
    }

    @Override
    public void tick() {
        if (!this.golem.isRemoved() && this.golem.hasActiveLaserTarget()) {
            this.x = (float) this.golem.getX();
            this.y = (float) this.golem.getY();
            this.z = (float) this.golem.getZ();
            float scale = this.golem.getLaserAttackScale(0.0F);
            this.volume = scale * scale;
            this.pitch = PITCH_MIN + PITCH_SCALE * scale;
        } else {
            this.stop();
        }
    }
}
