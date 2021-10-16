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

package androidx.compose.material

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
internal actual fun PlatformMaterialTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalScrollbarStyle provides defaultScrollbarStyle().copy(
            shape = MaterialTheme.shapes.small,
            unhoverColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
            hoverColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
        ),
        content = content
    )
}
