package com.github.steanky.ethylene.mapper.type;

import java.lang.reflect.Type;

/**
 * Marker interface used to denote a specialized subclass of {@link Type}. Used to prevent potential accidental misuse
 * of {@link GenericInfoRepository} by limiting which implementations of {@link Type} can be bound.
 * <p>
 * Implementation instances should generally only be used after binding to an "owner" class with
 * {@link GenericInfoRepository}.
 * <p>
 * This interface is not part of the public API.
 * <p>
 *
 * @implSpec In addition to complying with the specifications of Type, and any relevant sub-interfaces, implementations
 * must not retain any strong references to classloaders, whether directly or indirectly. As a side effect of this, it
 * is expected that certain methods that rely on reference objects will throw unchecked exceptions if they are accessed
 * after the referent(s) have been garbage collected. Notably, this includes {@link Object#equals(Object)} and
 * {@link Object#hashCode()}.
 * @see GenericInfoRepository
 * @see InternalGenericArrayType
 * @see InternalParameterizedType
 */
sealed interface WeakType extends Type permits InternalGenericArrayType, InternalParameterizedType {
}
