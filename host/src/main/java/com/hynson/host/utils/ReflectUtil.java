package com.hynson.host.utils;

import java.lang.reflect.Field;

/**
 * Created by hynson on 2020/12/23.
 */

public class ReflectUtil {

    /**
     * className 不公用
     * @param className
     * @param fieldName
     * @param access
     * @return
     * @throws Exception
     */
    public static Field getField(String className, String fieldName,boolean access) throws Exception {
        Class<?> clazz = Class.forName(className);
        Field field = access ? clazz.getField(fieldName) : clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    /**
     * clazz 外部公用
     * @param clazz
     * @param fieldName
     * @param access
     * @return
     * @throws Exception
     */
    public static Field getField(Class clazz, String fieldName, boolean access) throws Exception {
        Field field = access ? clazz.getField(fieldName) : clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    /**
     * className不公用
     *
     * @param className
     * @param fieldName
     * @param access
     * @return
     * @throws Exception
     */
    public static Object get(String className,String fieldName,boolean access) throws Exception{
        Class<?> clazz = Class.forName(className);
        Field field = access ? clazz.getField(fieldName) : clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field.get(clazz);
    }

    public static Object get(Class clazz,String fieldName,boolean access) throws Exception{
        Field field = access ? clazz.getField(fieldName) : clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field.get(clazz);
    }

    public static Object get(Object obj, String fieldName,boolean access) throws Exception {
        Field field = access ? obj.getClass().getField(fieldName) : obj.getClass().getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field.get(obj);
    }

    public static void set(Class clazz, String fieldName, Object obj, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(obj, value);
    }
}
