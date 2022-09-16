package com.github.steanky.ethylene.mapper.type;

import java.lang.reflect.Type;

/**
 * Marker interface used to denote a custom subclass of {@link Type}. Used to prevent potential accidental misuse of
 * {@link GenericInfoRepository} by limiting which implementations of {@link Type} can be bound.<p>
 *
 * @implSpec In addition to complying with the specifications of Type, and any specific sub-interfaces, implementations
 * must not retain any strong references to {@link Class} objects, whether directly or indirectly.
 * @see GenericInfoRepository
 * @see InternalGenericArrayType
 * @see InternalParameterizedType
 */
interface CustomType extends Type {}
