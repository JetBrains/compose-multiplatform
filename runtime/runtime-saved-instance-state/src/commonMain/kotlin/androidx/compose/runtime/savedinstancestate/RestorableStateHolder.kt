/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime.savedinstancestate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Providers
import androidx.compose.runtime.key
import androidx.compose.runtime.onActive
import androidx.compose.runtime.remember

@RequiresOptIn(
    "This is an experimental API. This means that the API is not yet stable and can be" +
        "changed before being promoted to stable."
)
annotation class ExperimentalRestorableStateHolder

/**
 * Allows to save the state defined with [savedInstanceState] and [rememberSavedInstanceState]
 * for the subtree before disposing it to make it possible to compose it back next time with the
 * restored state. It allows different navigation patterns to keep the ui state like scroll
 * position for the currently not composed screens from the backstack.
 *
 * @sample androidx.compose.runtime.savedinstancestate.samples.SimpleNavigationWithRestorableStateSample
 *
 * The content should be composed using [RestorableStateProvider] while providing a key representing
 * this content. Next time [RestorableStateProvider] will be used with the same key its state will be
 * restored.
 *
 * @param T type of the keys. Note that on Android you can only use types which can be stored
 * inside the Bundle.
 */
@ExperimentalRestorableStateHolder
interface RestorableStateHolder<T : Any> {
    /**
     * Put your content associated with a [key] inside the [content]. This will automatically
     * save all the states defined with [savedInstanceState] and [rememberSavedInstanceState]
     * before disposing the content and will restore the states when you compose with this key
     * again.
     *
     * @param key to be used for saving and restoring the states for the subtree. Note that on
     * Android you can only use types which can be stored inside the Bundle.
     */
    @Composable
    fun RestorableStateProvider(key: T, content: @Composable () -> Unit)

    /**
     * Removes the saved state associated with the passed [key].
     */
    fun removeState(key: T)
}

/**
 * Creates and remembers the instance of [RestorableStateHolder].
 *
 * @param T type of the keys. Note that on Android you can only use types which can be stored
 * inside the Bundle.
 */
@ExperimentalRestorableStateHolder
@Composable
fun <T : Any> rememberRestorableStateHolder(): RestorableStateHolder<T> =
    rememberSavedInstanceState(
        saver = RestorableStateHolderImpl.Saver()
    ) {
        RestorableStateHolderImpl<T>()
    }.apply {
        parentSavedStateRegistry = AmbientUiSavedStateRegistry.current
    }

@ExperimentalRestorableStateHolder
private class RestorableStateHolderImpl<T : Any>(
    private val savedStates: MutableMap<T, Map<String, List<Any?>>> = mutableMapOf()
) : RestorableStateHolder<T> {
    private val registryHolders = mutableMapOf<T, RegistryHolder>()
    var parentSavedStateRegistry: UiSavedStateRegistry? = null

    @OptIn(ExperimentalComposeApi::class)
    @Composable
    override fun RestorableStateProvider(key: T, content: @Composable () -> Unit) {
        key(key) {
            val registryHolder = remember {
                require(parentSavedStateRegistry?.canBeSaved(key) ?: true) {
                    "Type of the key used for withRestorableState is not supported. On Android " +
                        "you can only use types which can be stored inside the Bundle."
                }
                RegistryHolder(key)
            }
            Providers(
                AmbientUiSavedStateRegistry provides registryHolder.registry,
                content = content
            )
            onActive {
                require(key !in registryHolders)
                savedStates -= key
                registryHolders[key] = registryHolder
                onDispose {
                    registryHolder.saveTo(savedStates)
                    registryHolders -= key
                }
            }
        }
    }

    private fun saveAll(): MutableMap<T, Map<String, List<Any?>>> {
        val map = savedStates.toMutableMap()
        registryHolders.values.forEach { it.saveTo(map) }
        return map
    }

    override fun removeState(key: T) {
        val registryHolder = registryHolders[key]
        if (registryHolder != null) {
            registryHolder.shouldSave = false
        } else {
            savedStates -= key
        }
    }

    inner class RegistryHolder constructor(
        val key: T
    ) {
        var shouldSave = true
        val registry: UiSavedStateRegistry = UiSavedStateRegistry(savedStates[key]) {
            parentSavedStateRegistry?.canBeSaved(it) ?: true
        }

        fun saveTo(map: MutableMap<T, Map<String, List<Any?>>>) {
            if (shouldSave) {
                map[key] = registry.performSave()
            }
        }
    }

    companion object {
        private val Saver: Saver<RestorableStateHolderImpl<Any>, *> = Saver(
            save = { it.saveAll() },
            restore = { RestorableStateHolderImpl(it) }
        )

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Saver() = Saver as Saver<RestorableStateHolderImpl<T>, *>
    }
}
