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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text.input

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.textInputServiceFactory
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.AndroidTextInputServicePlugin.Adapter

/**
 * The [PlatformTextInputAdapter] that is responsible for creating [TextInputService]s and bridging
 * from Android APIs to [TextInputService] APIs.
 *
 * If some of this code seems unnecessarily complex, it's because this layer was introduced after
 * the rest of the text system was built in order to allow us to move the entirety of the text
 * implementation to a different module. The original [TextInputService] infrastructure was adapted
 * as-is.
 *
 * For example, this object uses both the [TextInputService] and the platform-specific
 * [TextInputServiceAndroid] as the "service" object because the android-specific APIs it needs to
 * delegate to are only available on the latter, but it needs to have access to the former as well
 * to support [PlatformTextInputAdapter.inputForTests].
 */
internal object AndroidTextInputServicePlugin : PlatformTextInputPlugin<Adapter> {

    @OptIn(InternalComposeUiApi::class)
    override fun createAdapter(platformTextInput: PlatformTextInput, view: View): Adapter {
        val platformService = TextInputServiceAndroid(view, platformTextInput)
        // This indirection is used for tests (see testInput above). This could be cleaned up now
        // that both halves live in the same class, but not worth the refactoring given the text
        // field api rewrite.
        val service = textInputServiceFactory(platformService)
        return Adapter(service, platformService)
    }

    class Adapter(
        val service: TextInputService,
        private val androidService: TextInputServiceAndroid
    ) : PlatformTextInputAdapter {

        override val inputForTests: TextInputForTests
            get() = service as? TextInputForTests
                ?: error("Text input service wrapper not set up! Did you use ComposeTestRule?")

        override fun createInputConnection(outAttrs: EditorInfo): InputConnection =
            androidService.createInputConnection(outAttrs)
    }
}