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
 * Default duration of a key press in milliseconds (duration between key down and key up).
 */
const val DefaultKeyPressDuration = 50L // milliseconds

/**
 * Default duration of the pause between sequential key presses in milliseconds.
 */
const val DefaultPauseDurationBetweenKeyPresses = 50L // milliseconds

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
     * Indicates whether caps lock is on or not.
     *
     * Note that this reflects the state of the injected input only, it does not correspond to the
     * state of an actual keyboard attached to the device on which a test is run
     */
    val isCapsLockOn: Boolean

    /**
     * Indicates whether num lock is on or not.
     *
     * Note that this reflects the state of the injected input only, it does not correspond to the
     * state of an actual keyboard attached to the device on which a test is run
     */
    val isNumLockOn: Boolean

    /**
     * Indicates whether scroll lock is on or not.
     *
     * Note that this reflects the state of the injected input only, it does not correspond to the
     * state of an actual keyboard attached to the device on which a test is run
     */
    val isScrollLockOn: Boolean

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

    override val isCapsLockOn: Boolean get() = inputDispatcher.isCapsLockOn
    override val isNumLockOn: Boolean get() = inputDispatcher.isNumLockOn
    override val isScrollLockOn: Boolean get() = inputDispatcher.isScrollLockOn

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
 */
@ExperimentalTestApi
fun KeyInjectionScope.pressKey(key: Key, pressDuration: Long = DefaultKeyPressDuration) {
    keyDown(key)
    advanceEventTime(pressDuration)
    keyUp(key)
}

/**
 * Presses the given [key] the given number of [times], for [pressDuration] milliseconds each time.
 * Pauses for [pauseDuration] milliseconds in between each key press.
 *
 * If the given [key] is already down an [IllegalStateException] is thrown.
 *
 * @param key The key to be pressed.
 * @param times The number of times to press the given key.
 * @param pressDuration The length of time for which to hold each key press.
 * @param pauseDuration The duration of the pause in between presses.
 */
@ExperimentalTestApi
fun KeyInjectionScope.pressKey(
    key: Key,
    times: Int,
    pressDuration: Long = DefaultKeyPressDuration,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses
) = (0 until times).forEach { idx ->
    if (idx != 0) advanceEventTime(pauseDuration)
    pressKey(key, pressDuration)
}

/**
 * Holds down the key each of the given [keys] for the given [pressDuration] in sequence, with
 * [pauseDuration] milliseconds between each press.
 *
 * If one of the keys is already down, an [IllegalStateException] will be thrown.
 *
 * @param keys The list of keys to be pressed down.
 * @param pressDuration Duration of press in milliseconds.
 * @param pauseDuration The duration between presses.
 */
@ExperimentalTestApi
fun KeyInjectionScope.pressKeys(
    keys: List<Key>,
    pressDuration: Long = DefaultKeyPressDuration,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses
) = keys.forEachIndexed { idx: Int, key: Key ->
    if (idx != 0) advanceEventTime(pauseDuration)
    pressKey(key, pressDuration)
}
