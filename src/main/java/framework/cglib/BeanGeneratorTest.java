package framework.cglib;

import net.sf.cglib.beans.BeanGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BeanGeneratorTest {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        BeanGenerator generator = new BeanGenerator();
        generator.addProperty("value", String.class);
        Object bean = generator.create();
        Method setter = bean.getClass().getMethod("setValue", String.class);
        setter.invoke(bean, "hello");

        Method getter = bean.getClass().getMethod("getValue");
        String value = (String) getter.invoke(bean);
        System.out.print(value);
    }
}
