import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.BlockPos;
public abstract class TestMob extends PathfinderMob {
    protected TestMob(net.minecraft.world.entity.EntityType<? extends PathfinderMob> a, net.minecraft.world.level.Level b) { super(a,b); }
    public void test() {
        this.restrictTo(BlockPos.ZERO, 3);
        this.clearRestriction();
    }
}
