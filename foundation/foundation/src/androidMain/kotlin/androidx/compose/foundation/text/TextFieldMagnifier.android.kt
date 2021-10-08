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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MagnifierStyle
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier {
    // Avoid tracking animation state on older Android versions that don't support magnifiers.
    if (!MagnifierStyle.TextDefault.isSupported) {
        return Modifier
    }

    return textFieldMagnifierAndroidImpl(
        manager = manager,
        androidMagnifier = { center ->
            Modifier.magnifier(
                sourceCenter = { center() },
                // TODO(b/202451044) Support fisheye magnifier for eloquent.
                style = MagnifierStyle.TextDefault
            )
        }
    )
}
