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
import java.util.function.Supplier;

/**
 * Weak version of {@link TypeVariable} which does not retain strong references to any {@link Type} objects.
 * <p>
 * This class violates the contract of {@link TypeVariable}, which specifies that its creation must not reify its
 * bounds. Said bounds are resolved in the constructor. We can't lazily evaluate the bounds without retaining a strong
 * reference to Type objects (violating the contract of {@link WeakType}) or risking early garbage collection (there is
 * no guarantee that a TypeVariable is cached by the JVM).
 * @param <TDec> the type of generic declaration
 */
final class WeakTypeVariable<TDec extends GenericDeclaration> extends WeakTypeBase implements WeakType, TypeVariable<TDec> {
    private final int variableIndex;
    private final Reference<Type>[] boundReferences;
    private final String[] boundReferenceNames;
    private final Supplier<? extends TDec> genericDeclarationSupplier;
    private final String name;

    @SuppressWarnings("unchecked")
    WeakTypeVariable(@NotNull TypeVariable<TDec> variable, int variableIndex) {
        super(generateIdentifier(variable, variableIndex));
        this.variableIndex = variableIndex;

        Type[] bounds = variable.getBounds();
        this.boundReferences = new Reference[bounds.length];
        this.boundReferenceNames = new String[bounds.length];

        TDec genericDeclaration = variable.getGenericDeclaration();
        if (genericDeclaration instanceof Class<?> type) {
            //simple case: generic decl is class
            Reference<Class<?>> genericDeclarationReference = new WeakReference<>(type);
            String className = type.getTypeName();
            GenericInfo.populate(bounds, boundReferences, boundReferenceNames, this, type.getClassLoader());
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
            GenericInfo.populate(bounds, boundReferences, boundReferenceNames, this, declaringClass
                .getClassLoader());
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

                //re-set the cached value
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

    static byte @NotNull [] generateIdentifier(@NotNull TypeVariable<?> variable, int variableIndex) {
        Type[] bounds = variable.getBounds();
        GenericDeclaration declaration = variable.getGenericDeclaration();
        if (declaration instanceof Class<?> type) {
            Type[] merged = new Type[bounds.length + 1];
            merged[0] = type;
            System.arraycopy(bounds, 0, merged, 1, bounds.length);
            return GenericInfo.identifier(GenericInfo.TYPE_VARIABLE, Integer.toString(variableIndex), merged);
        }
        else if (declaration instanceof Executable executable) {
            Class<?>[] parameters = executable.getParameterTypes();

            Type[] merged = new Type[bounds.length + parameters.length + 5];
            merged[0] = executable.getDeclaringClass();
            merged[1] = null;
            System.arraycopy(bounds, 0, merged, 2, bounds.length);
            merged[bounds.length] = null;
            System.arraycopy(parameters, 0, merged, 3 + bounds.length, parameters.length);
            merged[parameters.length + bounds.length + 3] = null;
            merged[parameters.length + bounds.length + 4] = executable.getDeclaringClass();
            return GenericInfo.identifier(GenericInfo.TYPE_VARIABLE, (executable instanceof Method ? "M" : "C")
                + executable.getName() + variableIndex, merged);
        }
        else {
            throw new IllegalArgumentException("Unexpected subclass of GenericDeclaration '" + declaration
                .getClass() + "'");
        }
    }

    private TypeVariable<?> resolveTypeVariable() {
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
        return resolveTypeVariable().getAnnotatedBounds();
    }

    @Override
    public <TAnno extends Annotation> TAnno getAnnotation(@NotNull Class<TAnno> annotationClass) {
        return resolveTypeVariable().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return resolveTypeVariable().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return resolveTypeVariable().getDeclaredAnnotations();
    }
}
