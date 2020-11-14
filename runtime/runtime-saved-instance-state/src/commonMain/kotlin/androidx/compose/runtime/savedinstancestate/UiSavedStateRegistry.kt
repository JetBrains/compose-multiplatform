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

import androidx.compose.runtime.staticAmbientOf

/**
 * Allows components to save and restore their state using the saved instance state mechanism.
 */
interface UiSavedStateRegistry {
    /**
     * Returns the restored value for the given key.
     * Once being restored the value is cleared, so you can't restore the same key twice.
     *
     * @param key Key used to save the value
     */
    fun consumeRestored(key: String): Any?

    /**
     * Registers the value provider.
     *
     * There are could be multiple providers registered for the same [key]. In this case the
     * order in which they were registered matters.
     *
     * Say we registered two providers for the key. One provides "1", second provides "2".
     * [performSave] in this case will have listOf("1", "2) as a value for the key in the map.
     * And later, when the registry will be recreated with the previously saved values, the first
     * execution of [consumeRestored] would consume "1" and the second one "2".
     *
     * @param key Key to use for storing the value
     * @param valueProvider Provides the current value, to be executed when [performSave]
     * will be triggered to collect all the registered values
     */
    fun registerProvider(key: String, valueProvider: () -> Any?)

    /**
     * Unregisters the value provider previously registered via [registerProvider].
     *
     * @param key Key of the value which shouldn't be saved anymore
     * @param valueProvider The provider previously passed to [registerProvider]
     */
    fun unregisterProvider(key: String, valueProvider: () -> Any?)

    /**
     * Returns true if the value can be saved using this Registry.
     * The default implementation will return true if this value can be stored in Bundle.
     *
     * @param value The value which we want to save using this Registry
     */
    fun canBeSaved(value: Any): Boolean

    /**
     * Executes all the registered value providers and combines these values into a map. We have
     * a list of values for each key as it is allowed to have multiple providers for the same key.
     */
    fun performSave(): Map<String, List<Any?>>
}

/**
 * Creates [UiSavedStateRegistry].
 *
 * @param restoredValues The map of the restored values
 * @param canBeSaved Function which returns true if the given value can be saved by the registry
 */
fun UiSavedStateRegistry(
    restoredValues: Map<String, List<Any?>>?,
    canBeSaved: (Any) -> Boolean
): UiSavedStateRegistry = UiSavedStateRegistryImpl(restoredValues, canBeSaved)

/**
 * Ambient with a current [UiSavedStateRegistry] instance.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientUiSavedStateRegistry",
    replaceWith = ReplaceWith(
        "AmbientUiSavedStateRegistry",
        "androidx.compose.runtime.savedinstancestate.AmbientUiSavedStateRegistry"
    )
)
val UiSavedStateRegistryAmbient get() = AmbientUiSavedStateRegistry

/**
 * Ambient with a current [UiSavedStateRegistry] instance.
 */
val AmbientUiSavedStateRegistry = staticAmbientOf<UiSavedStateRegistry?> { null }

private class UiSavedStateRegistryImpl(
    restored: Map<String, List<Any?>>?,
    private val canBeSaved: (Any) -> Boolean
) : UiSavedStateRegistry {

    private val restored: MutableMap<String, List<Any?>> =
        restored?.toMutableMap() ?: mutableMapOf()
    private val valueProviders = mutableMapOf<String, MutableList<() -> Any?>>()

    override fun canBeSaved(value: Any): Boolean = canBeSaved.invoke(value)

    override fun consumeRestored(key: String): Any? {
        val list = restored.remove(key)
        return if (list != null && list.isNotEmpty()) {
            if (list.size > 1) {
                restored[key] = list.subList(1, list.size)
            }
            list[0]
        } else {
            null
        }
    }

    override fun registerProvider(key: String, valueProvider: () -> Any?) {
        require(key.isNotBlank()) { "Registered key is empty or blank" }
        @Suppress("UNCHECKED_CAST")
        valueProviders.getOrPut(key) { mutableListOf() }.add(valueProvider)
    }

    override fun unregisterProvider(key: String, valueProvider: () -> Any?) {
        val list = valueProviders.remove(key)
        val found = list?.remove(valueProvider)
        require(found == true) {
            "The given key $key , valueProvider pair wasn't previously registered"
        }
        if (list.isNotEmpty()) {
            // if there are other providers for this key return list back to the map
            valueProviders[key] = list
        }
    }

    override fun performSave(): Map<String, List<Any?>> {
        val map = restored.toMutableMap()
        valueProviders.forEach { (key, list) ->
            if (list.size == 1) {
                val value = list[0].invoke()
                if (value != null) {
                    check(canBeSaved(value))
                    map[key] = arrayListOf<Any?>(value)
                }
            } else {
                // if we have multiple providers we should store null values as well to preserve
                // the order in which providers were registered. say there were two providers.
                // the first provider returned null(nothing to save) and the second one returned
                // "1". when we will be restoring the first provider would restore null (it is the
                // same as to have nothing to restore) and the second one restore "1".
                map[key] = list.map {
                    val value = it.invoke()
                    if (value != null) {
                        check(canBeSaved(value))
                    }
                    value
                }
            }
        }
        return map
    }
}
