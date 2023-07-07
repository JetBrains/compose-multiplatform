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

package androidx.compose.ui.node

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.viewinterop.InteropView

/**
 * This interface allows the layout inspector to access inspect instances of Views that are
 * associated with a Compose UI node. Usage of this API outside of Compose UI artifacts is
 * unsupported.
 */
@InternalComposeUiApi
sealed interface InteroperableComposeUiNode {
    fun getInteropView(): InteropView?
}