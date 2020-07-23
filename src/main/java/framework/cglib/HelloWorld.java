package framework.cglib;

import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Arrays;

public class HelloWorld {
    private String message;

    public HelloWorld(String message) {
        this.message = message;
        System.out.println("构造方法被调用: " + hashCode());
        System.out.println("构造方法中: " + getClass().getName());
    }

    public void hello() {
        System.out.println("Hello World");
    }

    @Override
    public String toString() {
        System.out.println("to string");
        return getClass().getName() + "@" + hashCode();
    }

    public static void main(final String[] args) {
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "cglib");
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(HelloWorld.class);
        enhancer.setCallback(new MethodInterceptor() {
            public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    return methodProxy.invokeSuper(o, args);
                }
                System.out.println("\nmethod: " + method.getName());
                System.out.println("\ndeclaring class: " + method.getDeclaringClass().getName());
                System.out.println("args: " + Arrays.toString(args));
                System.out.println("Before method run...");
                Object result = methodProxy.invokeSuper(o, args);
                System.out.println("After method run...\n");
                return result;
            }
        });

        HelloWorld helloWorld = (HelloWorld) enhancer.create(new Class[] {String.class}, new Object[] {"world"});
        helloWorld.hello();
        System.out.println(helloWorld.getClass().getName());
        System.out.println(helloWorld.hashCode());
        System.out.println(helloWorld.toString());
    }
}
