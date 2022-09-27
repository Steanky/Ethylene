package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * Marker interface used to denote a specialized subclass of {@link Type}. Used to prevent potential accidental misuse
 * of {@link GenericInfo} by limiting which implementations of {@link Type} can be bound.
 * <p>
 * Implementation instances should generally only be used after binding to an "owner" class with {@link GenericInfo}.
 * <p>
 * This interface is not part of the public API.
 * <p>
 *
 * @implSpec In addition to complying with the specifications of Type, and any relevant sub-interfaces, implementations
 * must not retain any strong references to classloaders, whether directly or indirectly. As a side effect of this, it
 * is expected that certain methods that rely on reference objects will throw unchecked exceptions if they are accessed
 * after the referent(s) have been garbage collected.
 * <p>
 * All WeakType implementations must override {@link Object#equals(Object)} and {@link Object#hashCode()}. Each
 * implementation must check for equality using only its identifier. A WeakType's hash code must not change over its
 * lifetime at any point.
 * @see GenericInfo
 * @see WeakTypeBase
 * @see WeakGenericArrayType
 * @see WeakParameterizedType
 * @see WeakTypeVariable
 * @see WeakWildcardType
 */
sealed interface WeakType extends Type permits WeakGenericArrayType, WeakParameterizedType, WeakTypeBase,
    WeakTypeVariable, WeakWildcardType {
    /**
     * The identifier bytes, used to compare instances for equality. The format this uses is specified by
     * {@link GenericInfo#identifier(byte, String, Type...)}.
     * <p>
     * To improve performance, implementations are not required to create defensive copies of their unique identifier
     * array. Unless otherwise specified, callers should assume this array is not safe to modify.
     *
     * @return the unique identifier array, which must not be modified
     */
    byte @NotNull [] identifier();
}
