/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose

import kotlin.reflect.KClass

/**
 * @UnionType can be used when an attribute can be one of several different types.
 * The annotation indicates that the Kotlin type has been expanded to a common
 * subtype (often Any) for the purpose of accepting one of a multiplicity of
 * subtypes.  The @UnionType must specify the intended subtypes, which can then be enforced
 * by the compiler.  @UnionType is particularly useful when a composable function
 * is wrapping a native View because it allows a composable to accept the multiplicity
 * of types corresponding to the overloaded setters for that attribute.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class UnionType(vararg val types: KClass<*>)
