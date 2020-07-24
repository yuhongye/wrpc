package com.cxy.wrpc.utils;

import com.cxy.wrpc.annotations.AnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BeanFactory {
    public static List<Object> getBeanWithAnnotation(String basePackage, Class<? extends Annotation> annotation) {
        Objenesis objenesis = new ObjenesisStd(true);
        Set<Class<?>> clses = AnnotationScanner.scan(basePackage, annotation);
        log.info("base package:{}, with annotation: {}", basePackage, annotation.getName(),
                clses.stream().map(Class::getName).collect(Collectors.joining(",")));
        return clses.stream().map(objenesis::newInstance).collect(Collectors.toList());
    }
}
