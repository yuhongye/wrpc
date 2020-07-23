package framework.cglib;

import net.sf.cglib.beans.ImmutableBean;

public class ImmutableBeanTest {
    public static void main(String[] args) {
        SampleBean bean = new SampleBean("hello");
        System.out.println(bean.getValue());

        SampleBean proxy = (SampleBean) ImmutableBean.create(bean);
        System.out.println("proxy value: " + proxy.getValue());

        bean.setValue("world");
        System.out.println(bean.getValue());
        System.out.println("proxy value: " + proxy.getValue());

        // 抛异常
        proxy.setValue("proxy");
    }
}
