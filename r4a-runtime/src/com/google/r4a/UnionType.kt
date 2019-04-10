package com.google.r4a

import kotlin.reflect.KClass

/**
 * @UnionType can be used when an attribute can be one of several different types.
 * The annotation indicates that the Kotlin type has been expanded to a common
 * subtype (often Any) for the purpose of accepting one of a multiplicity of
 * subtypes.  The @UnionType must specify the intended subtypes, which can then be enforced
 * by the compiler.  @UnionType is particularly useful when a composable function
 * is wrapping a native View because it allows a composable to accept the multiplicity
 * of types corresponding to the overloaded setters for that attribute.
 * Learn more at: https://goto.google.com/r4a-uniontype
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class UnionType(vararg val types: KClass<*>)

