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

package androidx.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.plus
import androidx.compose.ui.text.AnnotatedString

val SelectionTrackerAmbient = staticCompositionLocalOf<SelectionTracker> {
    error("CompositionLocal SelectionTrackerAmbient not provided")
}

class SelectionTracker {
    var getSelectedText: (() -> AnnotatedString?)? = null
}

val copyToClipboardKeySet by lazy {
    when (DesktopPlatform.Current) {
        DesktopPlatform.MacOS -> Key.MetaLeft + Key.C
        else -> Key.CtrlLeft + Key.C
    }
}