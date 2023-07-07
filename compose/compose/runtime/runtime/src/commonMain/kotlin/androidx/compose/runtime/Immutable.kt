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

package androidx.compose.runtime

/**
 * [Immutable] can be used to mark class as producing immutable instances. The immutability of the
 * class is not validated and is a promise by the type that all publicly accessible properties
 * and fields will not change after the instance is constructed. This is a stronger promise than
 * `val` as it promises that the value will never change not only that values cannot be changed
 * through a setter.
 *
 * [Immutable] is used by composition which enables composition optimizations that can be
 * performed based on the assumption that values read from the type will not change.  See
 * [StableMarker] for additional details.
 *
 * `data` classes that only contain `val` properties that do not have custom getters can safely
 * be marked as [Immutable] if the types of properties are either primitive types or also
 * [Immutable]:
 *
 * @sample androidx.compose.runtime.samples.simpleImmutableClass
 *
 * Marking `Person` immutable allows calls the `PersonView` [Composable] function to be skipped if
 * it is the same `person` as it was during the last composition.
 *
 * @see StableMarker
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@StableMarker
annotation class Immutable