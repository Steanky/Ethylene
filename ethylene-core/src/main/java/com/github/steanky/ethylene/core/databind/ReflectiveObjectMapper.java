package com.github.steanky.ethylene.core.databind;

import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.databind.annotations.Include;
import com.github.steanky.ethylene.core.databind.annotations.Name;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ReflectiveObjectMapper implements ObjectMapper {
    private final boolean accessPrivateConstructor = true;
    private final boolean accessPrivateFields = true;

    @Override
    public <TReturn> @NotNull TReturn mapNode(@NotNull ConfigNode element,
                                              @NotNull Class<TReturn> returnClass) {
        TReturn returnInstance = makeInstance(returnClass);
        Class<?> actualClass = returnInstance.getClass();

        boolean includeByDefault = actualClass.isAnnotationPresent(Include.class);
        for(Field field : actualClass.getFields()) {
            boolean canAccess = field.canAccess(returnInstance);
            String name = getName(field);

            if(includeByDefault) {

            }
        }

        return returnInstance;
    }

    @Override
    public @NotNull ConfigNode mapObject(@NotNull Object object) {
        return null;
    }

     private <TReturn> TReturn makeInstance(@NotNull Class<TReturn> returnClass) {
         Constructor<?>[] constructors = returnClass.getDeclaredConstructors();

         for(Constructor<?> constructor : constructors) {
             Class<?>[] parameterClasses = constructor.getParameterTypes();

             if(parameterClasses.length == 0) {
                 boolean canAccess = constructor.canAccess(null);

                 if(accessPrivateConstructor) {
                     if(!canAccess) {
                         constructor.setAccessible(true);
                     }
                 }
                 else if(!canAccess) {
                     continue;
                 }

                 try {
                     return returnClass.cast(constructor.newInstance());
                 }
                 catch (InvocationTargetException | InstantiationException | IllegalAccessException exception) {
                     throw new IllegalStateException("Reflection-related exception when trying to instantiate " +
                             returnClass.getName(), exception);
                 }
             }
         }

         throw new IllegalArgumentException("Unable to find a parameterless constructor for object of type " +
                 returnClass.getName());
     }

     private String getName(Field field) {
        var name = field.getAnnotation(Name.class);

        if(name != null) {
            return name.value();
        }
        else {
            return field.getName();
        }
     }
}
