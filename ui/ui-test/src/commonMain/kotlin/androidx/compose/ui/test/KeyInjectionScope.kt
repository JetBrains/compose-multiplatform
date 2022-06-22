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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Default duration of a key press in milliseconds (duration between key down and key up).
 */
@ExperimentalTestApi
private const val DefaultKeyPressDuration = 50L // milliseconds

/**
 * Default duration of the pause between sequential key presses in milliseconds.
 */
@ExperimentalTestApi
private const val DefaultPauseDurationBetweenKeyPresses = 50L // milliseconds

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
 * When a key is held down - i.e. the virtual clock is forwarded whilst the key is pressed down,
 * repeat key down events will be sent. In a fashion consistent with Android's implementation, the
 * first repeat key event will be sent after a key has been held down for 500ms. Subsequent repeat
 * events will be sent at 50ms intervals, until the key is released or another key is pressed down.
 *
 * The sending of repeat key events is handled as an implicit side-effect of [advanceEventTime],
 * which is called within the injection scope. As such, no repeat key events will be sent if
 * [MainTestClock.advanceTimeBy] is used to advance the time.
 *
 * @see InjectionScope
 */
@ExperimentalTestApi
@JvmDefaultWithCompatibility
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
 * Depresses each of the given [keys] sequentially, with [pauseDuration] milliseconds separating
 * each successive call to [KeyInjectionScope.keyDown].
 *
 * If [keys] contains any duplicate elements, an [IllegalArgumentException] will be thrown.
 * If any of the given [keys] is already down, an [IllegalStateException] will be thrown.
 *
 * @param keys The keys to be depressed.
 * @param pauseDuration The duration separating each key down event in milliseconds.
 */
// TODO(b/234011835): Refactor this and all functions that take List<Keys> to use vararg instead.
@ExperimentalTestApi
fun KeyInjectionScope.keysDown(
    keys: List<Key>,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses
) {
    require(keys.size == keys.distinct().size) {
        "List of keys must not contain any duplicates."
    }
    keys.forEachIndexed { idx: Int, key: Key ->
        if (idx != 0) advanceEventTime(pauseDuration)
        keyDown(key)
    }
}

/**
 * Releases each of the given [keys] sequentially, with [pauseDuration] milliseconds separating
 * each successive call to [KeyInjectionScope.keyUp].
 *
 * If [keys] contains any duplicate elements, an [IllegalArgumentException] will be thrown.
 * If any of the given [keys] is not down, an [IllegalStateException] will be thrown.
 *
 * @param keys The keys to be released.
 * @param pauseDuration The duration separating each key up event in milliseconds.
 */
@ExperimentalTestApi
fun KeyInjectionScope.keysUp(
    keys: List<Key>,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses
) {
    require(keys.size == keys.distinct().size) {
        "List of keys must not contain any duplicates."
    }
    keys.forEachIndexed { idx: Int, key: Key ->
        if (idx != 0) advanceEventTime(pauseDuration)
        keyUp(key)
    }
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

/**
 * Executes the keyboard sequence specified in the given [block], whilst holding down the
 * given [key]. This key must not be used within the [block]. Waits for [pauseDuration]
 * milliseconds after pressing the [key] down before it injects the [block]. Waits for the same
 * duration after injecting the [block] before it releases the [key].
 *
 * If the given [key] is already down, an [IllegalStateException] will be thrown.
 *
 * @param key The key to be held down during injection of the [block].
 * @param pauseDuration The pause in ms after the initial key down and before the final key up.
 * @param block Sequence of KeyInjectionScope methods to be injected with the given key down.
 */
@ExperimentalTestApi
fun KeyInjectionScope.withKeyDown(
    key: Key,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses,
    block: KeyInjectionScope.() -> Unit
) {
    keyDown(key)
    advanceEventTime(pauseDuration)
    block.invoke(this)
    advanceEventTime(pauseDuration)
    keyUp(key)
}

/**
 * Executes the keyboard sequence specified in the given [block], whilst holding down the each of
 * the given [keys]. These keys must not be used within the [block]. Waits for [pauseDuration]
 * milliseconds in between each key down and each key up event.
 *
 * If [keys] contains any duplicate elements, an [IllegalArgumentException] will be thrown.
 * If any of the given [keys] are already down, an [IllegalStateException] will be thrown.
 *
 * @param keys List of keys to be held down during injection of the [block].
 * @param pauseDuration The pause in milliseconds between each key down and key up event.
 * @param block Sequence of KeyInjectionScope methods to be injected with the given keys down.
 */
@ExperimentalTestApi
fun KeyInjectionScope.withKeysDown(
    keys: List<Key>,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses,
    block: KeyInjectionScope.() -> Unit
) {
    keysDown(keys, pauseDuration)
    advanceEventTime(pauseDuration)
    block.invoke(this)
    advanceEventTime(pauseDuration)
    keysUp(keys, pauseDuration)
}

/**
 * Executes the keyboard sequence specified in the given [block], in between presses to the
 * given [key]. This key can also be used within the [block], as long as it is not down at the end
 * of the block. There will be [pauseDuration] milliseconds after the initial press and before the
 * final press.
 *
 * If the given [key] is already down, an [IllegalStateException] will be thrown.
 *
 * @param key The key to be toggled around the injection of the [block].
 * @param pauseDuration The pause after the initial and before the final key presses.
 * @param block Sequence of KeyInjectionScope methods to be injected with the given key down.
 */
@ExperimentalTestApi
fun KeyInjectionScope.withKeyToggled(
    key: Key,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses,
    block: KeyInjectionScope.() -> Unit
) {
    pressKey(key)
    advanceEventTime(pauseDuration)
    block.invoke(this)
    advanceEventTime(pauseDuration)
    pressKey(key)
}

/**
 * Executes the keyboard sequence specified in the given [block], in between presses to the
 * given [keys]. These keys can also be used within the [block], as long as they are not down at
 * the end of the block. There will be [pauseDuration] milliseconds after the initial press and
 * before the final press.
 *
 * If any of the given [keys] are already down, an [IllegalStateException] will be thrown.
 *
 * @param keys The keys to be toggled around the injection of the [block].
 * @param pauseDuration The pause after the initial and before the final key presses.
 * @param block Sequence of KeyInjectionScope methods to be injected with the given keys down.
 */
@ExperimentalTestApi
fun KeyInjectionScope.withKeysToggled(
    keys: List<Key>,
    pauseDuration: Long = DefaultPauseDurationBetweenKeyPresses,
    block: KeyInjectionScope.() -> Unit
) {
    pressKeys(keys)
    advanceEventTime(pauseDuration)
    block.invoke(this)
    advanceEventTime(pauseDuration)
    pressKeys(keys)
}

/**
 * Verifies whether the function key is down.
 *
 * @return true if the function key is currently down, false otherwise.
 */
@ExperimentalTestApi
@get:ExperimentalTestApi
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
val KeyInjectionScope.isFnDown: Boolean
    get() = isKeyDown(Key.Function)

/**
 * Verifies whether either of the control keys are down.
 *
 * @return true if a control key is currently down, false otherwise.
 */
@ExperimentalTestApi
@get:ExperimentalTestApi
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
val KeyInjectionScope.isCtrlDown: Boolean
    get() = isKeyDown(Key.CtrlLeft) || isKeyDown(Key.CtrlRight)

/**
 * Verifies whether either of the alt keys are down.
 *
 * @return true if an alt key is currently down, false otherwise.
 */
@ExperimentalTestApi
@get:ExperimentalTestApi
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
val KeyInjectionScope.isAltDown: Boolean
    get() = isKeyDown(Key.AltLeft) || isKeyDown(Key.AltRight)

/**
 * Verifies whether either of the meta keys are down.
 *
 * @return true if a meta key is currently down, false otherwise.
 */
@ExperimentalTestApi
@get:ExperimentalTestApi
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
val KeyInjectionScope.isMetaDown: Boolean
    get() = isKeyDown(Key.MetaLeft) || isKeyDown(Key.MetaRight)

/**
 * Verifies whether either of the shift keys are down.
 *
 * @return true if a shift key is currently down, false otherwise.
 */
@ExperimentalTestApi
@get:ExperimentalTestApi
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
val KeyInjectionScope.isShiftDown: Boolean
    get() = isKeyDown(Key.ShiftLeft) || isKeyDown(Key.ShiftRight)
