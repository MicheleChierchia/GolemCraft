package com.golemcraft.golemcraftmod.entity.ai;

import com.golemcraft.golemcraftmod.entity.BaseGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class StatueGoal extends Goal {
    private final BaseGolemEntity golem;

    public StatueGoal(BaseGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return this.golem.getOxidationLevel() == 3;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.golem.setDeltaMovement(this.golem.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        this.golem.setYHeadRot(this.golem.yBodyRot);
        this.golem.setXRot(0);
    }
}
