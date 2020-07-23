package com.cxy.wrpc.annotations;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 获取使用了指定注解的所有类
 */
public final class AnnotationScanner {

    /**
     * 获取指定包下所用打上了annotation的注解
     * @param basePackage
     * @return
     */
    public static Set<Class<?>> scan(String basePackage, Class<? extends Annotation> annotation) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(annotation);
    }
}
