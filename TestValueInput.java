import net.minecraft.world.level.storage.ValueInput;
public class TestValueInput {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : ValueInput.class.getMethods()) {
            System.out.println(m);
        }
    }
}
