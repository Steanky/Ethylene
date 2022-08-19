package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Internal utility class used by {@link ConfigElement}.
 */
final class ConfigElementHelper {
    private ConfigElementHelper() { throw new AssertionError("Don't do this."); }

    private static String pathToString(@NotNull Object... pathString) {
        StringBuilder builder = new StringBuilder("'");
        for(int i = 0; i < pathString.length; i++) {
            builder.append(pathString[i]);
            if(i < pathString.length - 1) {
                builder.append('/');
            }
        }
        builder.append('\'');

        return builder.toString();
    }

    /**
     * Static utility method used by {@link ConfigElement}. Acts as a wrapper for
     * {@link ConfigElement#getElement(Object...)}, and will use the value supplied by the {@link Supplier} if the path
     * does not exist, or the {@link Function} used to evaluate whether the type conversion may safely occur returns
     * false. Otherwise, the type conversion Function will be called and its result returned.
     * @param element the element to perform the operation on
     * @param returnSupplier the supplier used to supply return values if the path or type is invalid
     * @param typeValidator the function used to determine if the element type is valid
     * @param typeGetter the function used to actually convert the element
     * @param path the path to resolve
     * @param <TReturn> the generic return value type
     * @return the value stored in element at path, or the default value produced by the supplier
     * @throws NullPointerException if any of the arguments are null
     * @throws IllegalArgumentException if path contains invalid types (any besides Integer or String)
     */
    static <TReturn> @NotNull TReturn getOrDefault(@NotNull ConfigElement element,
                                                   @NotNull Supplier<TReturn> returnSupplier,
                                                   @NotNull Function<ConfigElement, Boolean> typeValidator,
                                                   @NotNull Function<ConfigElement, TReturn> typeGetter,
                                                   @NotNull Object ... path) {
        ConfigElement newElement = element.getElement(path);

        if(newElement != null && typeValidator.apply(newElement)) {
            return typeGetter.apply(newElement);
        }
        else {
            return returnSupplier.get();
        }
    }

    /**
     * Works like {@link ConfigElementHelper#getOrDefault(ConfigElement, Supplier, Function, Function, Object...)}, but
     * throws a {@link ConfigProcessException} if the path is invalid or if the object cannot be converted to the
     * desired type.
     * @param element the element to perform the operation on
     * @param typeValidator the function used to determine if the element type is valid
     * @param typeGetter the function used to actually convert the element
     * @param path the path to resolve
     * @return the value stored in element at path
     * @throws ConfigProcessException if the path is invalid, or the path is valid but the type is not
     */
    static <TReturn> @NotNull TReturn getOrThrow(@NotNull ConfigElement element,
                                                 @NotNull Function<ConfigElement, Boolean> typeValidator,
                                                 @NotNull Function<ConfigElement, TReturn> typeGetter,
                                                 @NotNull Object ... path) throws ConfigProcessException {
        ConfigElement newElement = element.getElement(path);

        if(newElement != null) {
            if(typeValidator.apply(newElement)) {
                return typeGetter.apply(newElement);
            }

            throw new ConfigProcessException("ConfigElement " + newElement + " is of an invalid type");
        }

        throw new ConfigProcessException("Path " + pathToString(path) + " is invalid for " + element);
    }
}
