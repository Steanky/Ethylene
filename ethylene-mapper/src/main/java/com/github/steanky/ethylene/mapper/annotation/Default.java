package com.github.steanky.ethylene.mapper.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;
import com.github.steanky.ethylene.core.ConfigElement;

/**
 * This annotation may be used to define default values for serializable classes. This annotation may be placed three
 * different ways: on {@code public static} methods, on record accessor components, and directly on the class. Where the
 * annotation is placed defines how it is interpreted by Ethylene.
 * <p>
 * If placed on a class, the value of this annotation is interpreted as Propylene configuration (see
 * {@link ConfigElement#of(String)}). The top-level element type must be a map. Each key of the map is considered the
 * name of the field, and each corresponding value is the default value of that field. Example:
 * <pre>
 * {@code
 * @Default("""
 *     {
 *         name='default_name',
 *         age=0
 *     }
 *     """);
 * public record Example(String name, int age) {}
 * }
 * </pre>
 * Using a class-level default will exclude any other methods of setting defaults from being considered.
 * <p>
 * Additionally, this annotation can be placed directly on a record component. If so, its value is interpreted as
 * Propylene defining the default value for that component. Example:
 * <pre>
 * {@code
 * public record Example(@Default("'default_name'") String name, @Default(0) int age) {}
 * }
 * </pre>
 * Finally, this annotation can be defined on a {@code public static} accessor methods returning {@link ConfigElement}s.
 * If done this way, the value of the annotation is considered to be the field name, and the ConfigElement is the
 * default value. Example:
 * <pre>
 * {@code
 * public record Example(String name, int age) {
 *     @Default("name")
 *     public static ConfigElement defaultName() {
 *         return ConfigElement.of("default_name");
 *     }
 *
 *     @Default("age")
 *     public static ConfigElement defaultAge() {
 *         return ConfigElement.of(0);
 *     }
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Default {
    /**
     * The name of the parameter for which to supply a default for.
     * @return the name of the parameter
     */
    @NotNull String value();
}
