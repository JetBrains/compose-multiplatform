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

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.skiko.ClipboardManager as SkikoCLipboardManager

internal class PlatformClipboardManager : ClipboardManager {
    val skikoClipboardManager = SkikoCLipboardManager()

    override fun getText(): AnnotatedString? =
        skikoClipboardManager.getText()?.let { AnnotatedString(it) }

    override fun setText(annotatedString: AnnotatedString) {
        skikoClipboardManager.setText(annotatedString.text)
    }
}
