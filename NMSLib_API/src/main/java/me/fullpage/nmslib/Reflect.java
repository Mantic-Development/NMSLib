package me.fullpage.nmslib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Reflect {

    @Nullable
    public static Class<?> getClass(@NotNull String path, @NotNull String name) {
        return getClass(path + "." + name);
    }

    @Nullable
    public static Class<?> getInnerClass(@NotNull String path, @NotNull String name) {
        return getClass(path + "$" + name);
    }


    public static Object getFieldValue(@NotNull Object source, @NotNull String name) {
        try {
            Class<?> clazz = source instanceof Class<?> ? (Class<?>) source : source.getClass();
            Field field = getField(clazz, name);
            if (field == null) return null;

            field.setAccessible(true);
            return field.get(source);
        }
        catch (IllegalAccessException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static boolean setFieldValue(@NotNull Object source, @NotNull String name, @Nullable Object value) {
        try {
            boolean isStatic = source instanceof Class;
            Class<?> clazz = isStatic ? (Class<?>) source : source.getClass();

            Field field = getField(clazz, name);
            if (field == null) return false;

            field.setAccessible(true);
            field.set(isStatic ? null : source, value);
            return true;
        }
        catch (IllegalAccessException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public static Field getField(@NotNull Class<?> source, @NotNull String name) {
        try {
            return source.getDeclaredField(name);
        }
        catch (NoSuchFieldException exception) {
            Class<?> superClass = source.getSuperclass();
            return superClass == null ? null : getField(superClass, name);
        }
    }

    private static Class<?> getClass(@NotNull String path) {
        return getClass(path, true);
    }

    private static Class<?> getClass(@NotNull String path, boolean printError) {
        try {
            return Class.forName(path);
        }
        catch (ClassNotFoundException exception) {
            if (printError) exception.printStackTrace();
            return null;
        }
    }

    public static Method getMethod(@NotNull Class<?> source, @NotNull String name, @NotNull Class<?>... params) {
        try {
            return source.getDeclaredMethod(name, params);
        }
        catch (NoSuchMethodException exception) {
            Class<?> superClass = source.getSuperclass();
            return superClass == null ? null : getMethod(superClass, name);
        }
    }

    public static Object invokeMethod(@NotNull Method method, @Nullable Object by, @Nullable Object... param) {
        method.setAccessible(true);
        try {
            return method.invoke(by, param);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
