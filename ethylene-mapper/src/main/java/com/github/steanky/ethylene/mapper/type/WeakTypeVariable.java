package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Weak version of {@link TypeVariable} which does not retain strong references to any {@link Type} objects.
 * <p>
 * This class violates the contract of {@link TypeVariable}, which specifies that its creation must not reify its
 * bounds. Said bounds are resolved in the constructor. We can't lazily evaluate the bounds without retaining a strong
 * reference to Type objects (violating the contract of {@link WeakType}) or risking early garbage collection (there is
 * no guarantee that a
 * @param <TDec> the type of generic declaration
 */
public final class WeakTypeVariable<TDec extends GenericDeclaration> implements WeakType, TypeVariable<TDec> {
    private final int variableIndex;
    private final Reference<Type>[] boundReferences;
    private final String[] boundReferenceNames;
    private final Supplier<? extends TDec> genericDeclarationSupplier;
    private final String name;

    @SuppressWarnings("unchecked")
    WeakTypeVariable(@NotNull TypeVariable<TDec> variable, int variableIndex) {
        Objects.requireNonNull(variable);
        this.variableIndex = variableIndex;

        if (variable instanceof WeakTypeVariable<?>) {
            throw new IllegalArgumentException("Creating WeakTypeVariable from WeakTypeVariable");
        }

        Type[] bounds = variable.getBounds();
        this.boundReferences = new Reference[bounds.length];
        this.boundReferenceNames = new String[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            Type type = bounds[i];
            boundReferences[i] = new WeakReference<>(type);
            boundReferenceNames[i] = type.getTypeName();
        }

        TDec genericDeclaration = variable.getGenericDeclaration();
        if (genericDeclaration instanceof Class<?> type) {
            //simple case: generic decl is class
            Reference<Class<?>> genericDeclarationReference = new WeakReference<>(type);
            String className = type.getTypeName();
            this.genericDeclarationSupplier = () -> (TDec)ReflectionUtils.resolve(genericDeclarationReference, className);
        }
        else if(genericDeclaration instanceof Method || genericDeclaration instanceof Constructor<?>) {
            Executable executable = (Executable) genericDeclaration;

            //methods/ctors are more complex: retain enough information to resolve the executable + retain softref for
            //caching purposes
            Class<?> declaringClass = executable.getDeclaringClass();
            Reference<Class<?>> declaringClassReference = new WeakReference<>(declaringClass);
            String declaringClassName = declaringClass.getTypeName();

            String executableName = executable.getName();

            Class<?>[] parameterTypes = executable.getParameterTypes();
            Reference<Class<?>>[] parameterTypeReferences = new Reference[parameterTypes.length];
            String[] parameterTypeNames = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypeReferences.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                parameterTypeReferences[i] = new WeakReference<>(parameterType);
                parameterTypeNames[i] = parameterType.getTypeName();
            }

            Mutable<Reference<TDec>> executableReference = new MutableObject<>(new SoftReference<>(genericDeclaration));
            boolean isMethod = genericDeclaration instanceof Method;
            this.genericDeclarationSupplier = (Supplier<TDec>) () -> {
                TDec cachedDeclaration = executableReference.getValue().get();
                if (cachedDeclaration != null) {
                    return cachedDeclaration;
                }

                //the method may have been garbage collected (it is not cached elsewhere)
                //re-resolve it from the owner class, parameter types, and name
                Class<?> executableOwner = ReflectionUtils.resolve(declaringClassReference, declaringClassName);
                Class<?>[] executableParameters = ReflectionUtils.resolve(parameterTypeReferences, parameterTypeNames,
                    Class.class);

                Executable newExecutable;
                try {
                    newExecutable = isMethod ? executableOwner.getDeclaredMethod(executableName, executableParameters) :
                        executableOwner.getDeclaredConstructor(executableParameters);
                } catch (NoSuchMethodException e) {
                    //should generally not happen
                    throw new TypeNotPresentException(declaringClassName, e);
                }

                //cast is safe
                TDec newGenericDeclaration = (TDec) newExecutable;

                //update the cached value
                executableReference.setValue(new SoftReference<>(newGenericDeclaration));
                return newGenericDeclaration;
            };
        }
        else {
            //TypeVariableImpl only supports class, method, and ctor so this shouldn't happen with current JVM
            throw new IllegalArgumentException("Unsupported generic declaration type '" + genericDeclaration + "'");
        }

        this.name = variable.getName();
    }

    private TypeVariable<?> resolveVariable() {
        return genericDeclarationSupplier.get().getTypeParameters()[variableIndex];
    }

    @Override
    public Type[] getBounds() {
        return ReflectionUtils.resolve(boundReferences, boundReferenceNames, Type.class);
    }

    @Override
    public TDec getGenericDeclaration() {
        return genericDeclarationSupplier.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AnnotatedType[] getAnnotatedBounds() {
        return resolveVariable().getAnnotatedBounds();
    }

    @Override
    public <TAnno extends Annotation> TAnno getAnnotation(@NotNull Class<TAnno> annotationClass) {
        return resolveVariable().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return resolveVariable().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return resolveVariable().getDeclaredAnnotations();
    }
}
