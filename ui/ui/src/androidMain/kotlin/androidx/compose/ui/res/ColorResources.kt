/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.res

import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientContext

/**
 * Load a color resource.
 *
 * @param id the resource identifier
 * @return the color associated with the resource
 */
@Composable
fun colorResource(@ColorRes id: Int): Color {
    val context = AmbientContext.current
    return if (Build.VERSION.SDK_INT >= 23) {
        Color(context.resources.getColor(id, context.theme))
    } else {
        @Suppress("DEPRECATION")
        Color(context.resources.getColor(id))
    }
}