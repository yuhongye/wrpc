package com.cxy.wrpc.utils;

public class ClassTypes {
    /**
     * @return if obj is primitive or wrapper type return wrapper TYPE, else obj.getClass
     */
    public static Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
            default:
                return classType;
        }
    }

    public static Class[] getParameterTypes(Object... args) {
        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = ClassTypes.getClassType(args[i]);
        }
        return parameterTypes;
    }


}
