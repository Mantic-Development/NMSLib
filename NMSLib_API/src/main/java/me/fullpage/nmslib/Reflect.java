package me.fullpage.nmslib;

import java.lang.reflect.Field;

public class Reflect {

    public static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
            field.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            field.setAccessible(false);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
