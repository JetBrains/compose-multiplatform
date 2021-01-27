/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.saveable

/**
 * The [Saver] describes how the object of [Original] class can be simplified and converted into
 * something which is [Saveable].
 *
 * What types can be saved is defined by [SaveableStateRegistry], by default everything which can
 * be stored in the Bundle class can be saved.
 * The implementations can check that the provided value can be saved via [SaverScope.canBeSaved]
 *
 * You can pass the implementations of this class as a parameter for [rememberSaveable].
 *
 * @sample androidx.compose.runtime.saveable.samples.CustomSaverSample
 */
interface Saver<Original, Saveable : Any> {
    /**
     * Convert the value into a saveable one. If null is returned the value will not be saved.
     */
    fun SaverScope.save(value: Original): Saveable?

    /**
     * Convert the restored value back to the original Class. If null is returned the value will
     * not be restored and would be initialized again instead.
     */
    fun restore(value: Saveable): Original?
}

/**
 * The [Saver] describes how the object of [Original] class can be simplified and converted into
 * something which is [Saveable].
 *
 * What types can be saved is defined by [SaveableStateRegistry], by default everything which can
 * be stored in the Bundle class can be saved.
 * The implementations can check that the provided value can be saved via [SaverScope.canBeSaved]
 *
 * You can pass the implementations of this class as a parameter for [rememberSaveable].
 *
 * @sample androidx.compose.runtime.saveable.samples.CustomSaverSample
 *
 * @param save Defines how to convert the value into a saveable one. If null is returned the
 * value will not be saved.
 * @param restore Defines how to convert the restored value back to the original Class. If null
 * is returned the value will not be restored and would be initialized again instead.
 */
fun <Original, Saveable : Any> Saver(
    save: SaverScope.(value: Original) -> Saveable?,
    restore: (value: Saveable) -> Original?
): Saver<Original, Saveable> {
    return object : Saver<Original, Saveable> {
        override fun SaverScope.save(value: Original) = save.invoke(this, value)

        override fun restore(value: Saveable) = restore.invoke(value)
    }
}

/**
 * Scope used in [Saver.save].
 *
 * @see Saver
 */
fun interface SaverScope {
    /**
     * What types can be saved is defined by [SaveableStateRegistry], by default everything which can
     * be stored in the Bundle class can be saved.
     */
    fun canBeSaved(value: Any): Boolean
}

/**
 * The default implementation of [Saver] which does not perform any conversion.
 *
 * It is used by [rememberSaveable] by default.
 *
 * @see Saver
 */
fun <T> autoSaver(): Saver<T, Any> =
    @Suppress("UNCHECKED_CAST")
    (AutoSaver as Saver<T, Any>)

private val AutoSaver = Saver<Any?, Any>(
    save = { it },
    restore = { it }
)
