import net.neoforged.neoforge.registries.DeferredRegister;
public class TestDefReg {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : DeferredRegister.Items.class.getMethods()) {
            System.out.println(m);
        }
    }
}
