package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Marker interface used to denote a specialized subclass of {@link Type}. Used to prevent potential accidental misuse
 * of {@link GenericInfo} by limiting which implementations of {@link Type} can be bound.
 * <p>
 * Implementation instances should generally only be used after binding to an "owner" class with
 * {@link GenericInfo}.
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
    @Nullable ClassLoader getBoundClassloader();

    byte[] identifier();
}
