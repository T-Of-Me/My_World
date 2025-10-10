/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class IntrospectionUtil {
    public static boolean isJavaBeanCompliantSetter(Method method) {
        if (method == null) {
            return false;
        }
        if (method.getReturnType() != Void.TYPE) {
            return false;
        }
        if (!method.getName().startsWith("set")) {
            return false;
        }
        return method.getParameterCount() == 1;
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>[] args, boolean checkInheritance, boolean strictArgs) throws NoSuchMethodException {
        if (clazz == null) {
            throw new NoSuchMethodException("No class");
        }
        if (methodName == null || methodName.trim().equals("")) {
            throw new NoSuchMethodException("No method name");
        }
        Method method = null;
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length && method == null; ++i) {
            if (!methods[i].getName().equals(methodName) || !IntrospectionUtil.checkParams(methods[i].getParameterTypes(), args == null ? new Class[]{} : args, strictArgs)) continue;
            method = methods[i];
        }
        if (method != null) {
            return method;
        }
        if (checkInheritance) {
            return IntrospectionUtil.findInheritedMethod(clazz.getPackage(), clazz.getSuperclass(), methodName, args, strictArgs);
        }
        throw new NoSuchMethodException("No such method " + methodName + " on class " + clazz.getName());
    }

    public static Field findField(Class<?> clazz, String targetName, Class<?> targetType, boolean checkInheritance, boolean strictType) throws NoSuchFieldException {
        if (clazz == null) {
            throw new NoSuchFieldException("No class");
        }
        if (targetName == null) {
            throw new NoSuchFieldException("No field name");
        }
        try {
            Field field = clazz.getDeclaredField(targetName);
            if (strictType ? field.getType().equals(targetType) : field.getType().isAssignableFrom(targetType)) {
                return field;
            }
            if (checkInheritance) {
                return IntrospectionUtil.findInheritedField(clazz.getPackage(), clazz.getSuperclass(), targetName, targetType, strictType);
            }
            throw new NoSuchFieldException("No field with name " + targetName + " in class " + clazz.getName() + " of type " + targetType);
        }
        catch (NoSuchFieldException e) {
            return IntrospectionUtil.findInheritedField(clazz.getPackage(), clazz.getSuperclass(), targetName, targetType, strictType);
        }
    }

    public static boolean isInheritable(Package pack, Member member) {
        if (pack == null) {
            return false;
        }
        if (member == null) {
            return false;
        }
        int modifiers = member.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            return true;
        }
        if (Modifier.isProtected(modifiers)) {
            return true;
        }
        return !Modifier.isPrivate(modifiers) && pack.equals(member.getDeclaringClass().getPackage());
    }

    public static boolean checkParams(Class<?>[] formalParams, Class<?>[] actualParams, boolean strict) {
        int j;
        if (formalParams == null) {
            return actualParams == null;
        }
        if (actualParams == null) {
            return false;
        }
        if (formalParams.length != actualParams.length) {
            return false;
        }
        if (formalParams.length == 0) {
            return true;
        }
        if (strict) {
            for (j = 0; j < formalParams.length && formalParams[j].equals(actualParams[j]); ++j) {
            }
        } else {
            while (j < formalParams.length && formalParams[j].isAssignableFrom(actualParams[j])) {
                ++j;
            }
        }
        return j == formalParams.length;
    }

    public static boolean isSameSignature(Method methodA, Method methodB) {
        if (methodA == null) {
            return false;
        }
        if (methodB == null) {
            return false;
        }
        List<Class<Class<?>>> parameterTypesA = Arrays.asList(methodA.getParameterTypes());
        List<Class<?>> parameterTypesB = Arrays.asList(methodB.getParameterTypes());
        return methodA.getName().equals(methodB.getName()) && parameterTypesA.containsAll(parameterTypesB);
    }

    public static boolean isTypeCompatible(Class<?> formalType, Class<?> actualType, boolean strict) {
        if (formalType == null) {
            return actualType == null;
        }
        if (actualType == null) {
            return false;
        }
        if (strict) {
            return formalType.equals(actualType);
        }
        return formalType.isAssignableFrom(actualType);
    }

    public static boolean containsSameMethodSignature(Method method, Class<?> c, boolean checkPackage) {
        if (checkPackage && !c.getPackage().equals(method.getDeclaringClass().getPackage())) {
            return false;
        }
        boolean samesig = false;
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length && !samesig; ++i) {
            if (!IntrospectionUtil.isSameSignature(method, methods[i])) continue;
            samesig = true;
        }
        return samesig;
    }

    public static boolean containsSameFieldName(Field field, Class<?> c, boolean checkPackage) {
        if (checkPackage && !c.getPackage().equals(field.getDeclaringClass().getPackage())) {
            return false;
        }
        boolean sameName = false;
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length && !sameName; ++i) {
            if (!fields[i].getName().equals(field.getName())) continue;
            sameName = true;
        }
        return sameName;
    }

    protected static Method findInheritedMethod(Package pack, Class<?> clazz, String methodName, Class<?>[] args, boolean strictArgs) throws NoSuchMethodException {
        if (clazz == null) {
            throw new NoSuchMethodException("No class");
        }
        if (methodName == null) {
            throw new NoSuchMethodException("No method name");
        }
        Method method = null;
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length && method == null; ++i) {
            if (!methods[i].getName().equals(methodName) || !IntrospectionUtil.isInheritable(pack, methods[i]) || !IntrospectionUtil.checkParams(methods[i].getParameterTypes(), args, strictArgs)) continue;
            method = methods[i];
        }
        if (method != null) {
            return method;
        }
        return IntrospectionUtil.findInheritedMethod(clazz.getPackage(), clazz.getSuperclass(), methodName, args, strictArgs);
    }

    protected static Field findInheritedField(Package pack, Class<?> clazz, String fieldName, Class<?> fieldType, boolean strictType) throws NoSuchFieldException {
        if (clazz == null) {
            throw new NoSuchFieldException("No class");
        }
        if (fieldName == null) {
            throw new NoSuchFieldException("No field name");
        }
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (IntrospectionUtil.isInheritable(pack, field) && IntrospectionUtil.isTypeCompatible(fieldType, field.getType(), strictType)) {
                return field;
            }
            return IntrospectionUtil.findInheritedField(clazz.getPackage(), clazz.getSuperclass(), fieldName, fieldType, strictType);
        }
        catch (NoSuchFieldException e) {
            return IntrospectionUtil.findInheritedField(clazz.getPackage(), clazz.getSuperclass(), fieldName, fieldType, strictType);
        }
    }
}

