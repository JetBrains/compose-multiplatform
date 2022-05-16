/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.input.key.Key

/**
 * The receiver scope of the key input injection lambda from [performKeyInput].
 *
 * All sequences and patterns of key input can be expressed using the two fundamental methods of
 * this API - [keyDown] and [keyUp]. All other injection functions are provided as abstractions
 * built on top of these two methods in order to improve test code readability/maintainability and
 * decrease development time.
 *
 * The entire event injection state is shared between all `perform.*Input` methods, meaning you
 * can continue an unfinished key input sequence in a subsequent invocation of [performKeyInput]
 * or [performMultiModalInput].
 *
 * All events sent by these methods are batched together and sent as a whole after
 * [performKeyInput] has executed its code block.
 *
 * @see InjectionScope
 */
@ExperimentalTestApi
interface KeyInjectionScope : InjectionScope {

    /**
     * Default duration of a key press in milliseconds.
     */
    val defaultPressDuration: Long get() = 50 // milliseconds

    /**
     * Sends a key down event for the given [key].
     *
     * If the given key is already down, an [IllegalStateException] will be thrown.
     *
     * @param key The key to be pressed down.
     */
    fun keyDown(key: Key)

    /**
     * Sends a key up event for the given [key].
     *
     * If the given key is already up, an [IllegalStateException] will be thrown.
     *
     * @param key The key to be released.
     */
    fun keyUp(key: Key)

    /**
     * Checks if the given [key] is down.
     *
     * @param key The key to be checked.
     * @return true if the given [key] is pressed down, false otherwise.
     */
    fun isKeyDown(key: Key): Boolean
}

@ExperimentalTestApi
internal class KeyInjectionScopeImpl(
    private val baseScope: MultiModalInjectionScopeImpl
) : KeyInjectionScope, InjectionScope by baseScope {
    private val inputDispatcher get() = baseScope.inputDispatcher

    // TODO(b/233186704) Find out why KeyEvents not registered when injected together in batches.
    override fun keyDown(key: Key) {
        inputDispatcher.enqueueKeyDown(key)
        inputDispatcher.flush()
    }

    override fun keyUp(key: Key) {
        inputDispatcher.enqueueKeyUp(key)
        inputDispatcher.flush()
    }

    override fun isKeyDown(key: Key): Boolean = inputDispatcher.isKeyDown(key)
}

/**
 * Holds down the given [key] for the given [pressDuration] by sending a key down event,
 * advancing the event time and sending a key up event.
 *
 * If the given key is already down, an [IllegalStateException] will be thrown.
 *
 * @param key The key to be pressed down.
 * @param pressDuration Duration of press in milliseconds.
 * Defaults to [KeyInjectionScope.defaultPressDuration].
 */
@ExperimentalTestApi
fun KeyInjectionScope.pressKey(key: Key, pressDuration: Long = defaultPressDuration) {
    keyDown(key)
    advanceEventTime(pressDuration)
    keyUp(key)
}

/**
 * Holds down the key each of the given [keys] for the given [pressDuration] in sequence, with
 * [interPressDuration] milliseconds between each press.
 *
 * If one keys is already down, an [IllegalStateException] will be thrown.
 *
 * @param keys The list of keys to be pressed down.
 * @param pressDuration Duration of press in milliseconds.
 * @param interPressDuration The duration between presses.
 * Both durations default to [KeyInjectionScope.defaultPressDuration].
 */
@ExperimentalTestApi
fun KeyInjectionScope.pressKeys(
    keys: List<Key>,
    pressDuration: Long = defaultPressDuration,
    interPressDuration: Long = defaultPressDuration
) = keys.forEachIndexed { idx: Int, key: Key ->
    if (idx != 0) advanceEventTime(interPressDuration)
    pressKey(key, pressDuration)
}
