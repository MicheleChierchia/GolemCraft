import java.lang.reflect.Method;
public class TestMobReflection {
    public static void main(String[] args) throws Exception {
        Class<?> mobClass = Class.forName("net.minecraft.world.entity.Mob");
        for (Method m : mobClass.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + java.util.Arrays.toString(m.getParameterTypes()));
        }
    }
}
