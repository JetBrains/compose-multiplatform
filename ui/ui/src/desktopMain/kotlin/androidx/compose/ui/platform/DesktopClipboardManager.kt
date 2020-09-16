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

// TODO(demin): implement ClipboardManager
class DesktopClipboardManager : ClipboardManager {
    override fun getText(): AnnotatedString? {
        println("ClipboardManager.getText not implemented yet")
        return null
    }

    override fun setText(annotatedString: AnnotatedString) {
        println("ClipboardManager.setText not implemented yet")
    }
}