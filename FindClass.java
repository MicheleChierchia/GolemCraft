import java.lang.reflect.Method;
public class FindClass {
    public static void main(String[] args) throws Exception {
        Class<?> itemsClass = Class.forName("net.minecraft.world.item.Items");
        System.out.println("Items class found: " + itemsClass.getName());
        Class<?> swordClass = Class.forName("net.minecraft.world.item.SwordItem");
        System.out.println("SwordItem class found: " + swordClass.getName());
    }
}
