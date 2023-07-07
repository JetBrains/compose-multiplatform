/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * Defines a plugin to the Compose text input system. Instances of this interface should be
 * stateless singleton `object`s. They act as both:
 *  - factories to create instances of [PlatformTextInputAdapter] that know how to talk to
 *  platform-specific IME APIs, as well as
 *  - keys into the cache of adapter instances managed by the [PlatformTextInputPluginRegistry].
 *
 * To register a plugin with the system, call [PlatformTextInputPluginRegistry.rememberAdapter]
 * from a composable, probably an implementation of a text field. [createAdapter] will be called
 * if necessary to instantiate the adapter.
 *
 * Implementations are intended to be used only by your text editor implementation, and probably not
 * exposed as public API.
 */
@ExperimentalTextApi
@Immutable
actual fun interface PlatformTextInputPlugin<T : PlatformTextInputAdapter> {
    /**
     * Creates a new instance of a [PlatformTextInputAdapter] hosted by [view].
     *
     * The [PlatformTextInputAdapter] implementation should call
     * [PlatformTextInput.requestInputFocus] when it's ready to start processing text input in order
     * to become the active delegate for the platform's IME APIs, and
     * [PlatformTextInput.releaseInputFocus] to notify the platform that it is no longer processing
     * input.
     */
    fun createAdapter(platformTextInput: PlatformTextInput, view: View): T
}

/**
 * An adapter for platform-specific IME APIs to implement a text editor. Instances of this interface
 * should be created by implementing a singleton [PlatformTextInputPlugin] `object` and passing it
 * to [PlatformTextInputPluginRegistry.rememberAdapter]. Instances will be created lazily and cached
 * as long as they are used at least once in a given composition. This allows implementations to
 * coordinate state between different text fields.
 *
 * Implementations of this interface are expected to:
 * - Call [PlatformTextInput.requestInputFocus] on the [PlatformTextInput] passed to the adapter's
 *  [PlatformTextInputPlugin] when they are ready to begin processing text input. Platform APIs will
 *  not be delegated to an adapter unless it holds input focus.
 * - Implement [createInputConnection] to create an [InputConnection] that talks to the IME.
 * - Return a [TextInputForTests] instance from [inputForTests] that implements text operations
 *  defined by the Compose UI testing framework.
 * - Optionally implement [onDisposed] to clean up any resources when the adapter is no longer used
 *  in the composition and will be removed from the [PlatformTextInputPluginRegistry]'s cache.
 *
 * Implementations are intended to be used only by your text editor implementation, and probably not
 * exposed as public API. Your adapter can define whatever internal API it needs to communicate with
 * the rest of your text editor code.
 */
@ExperimentalTextApi
actual interface PlatformTextInputAdapter {
    // TODO(b/267235947) When fleshing out the desktop actual, we might want to pull some of these
    //  members up into the expect interface (e.g. maybe inputForTests).

    /**
     * The [TextInputForTests] used to inject text editing commands by the testing framework.
     * This should only be called from tests, never in production.
     */
    val inputForTests: TextInputForTests?

    /** Delegate for [View.onCreateInputConnection]. */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection?

    /**
     * Called when this adapter is not remembered by any composables is removed from the
     * [PlatformTextInputPluginRegistry].
     */
    fun onDisposed() {}
}

@OptIn(ExperimentalTextApi::class)
internal actual fun PlatformTextInputAdapter.dispose() {
    onDisposed()
}