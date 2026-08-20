import net.minecraft.world.item.SpawnEggItem;
public class TestClass {
    public static void main(String[] args) {
        for (java.lang.reflect.Constructor<?> c : SpawnEggItem.class.getConstructors()) {
            System.out.println(c);
        }
    }
}
