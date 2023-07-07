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

package androidx.compose.ui.uikit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composition local for SafeArea of ComposeUIViewController
 */
@InternalComposeApi
val LocalSafeAreaState = staticCompositionLocalOf<State<IOSInsets>> {
    error("CompositionLocal LocalSafeAreaTopState not present")
}

/**
 * Composition local for layoutMargins of ComposeUIViewController
 */
@InternalComposeApi
val LocalLayoutMarginsState = staticCompositionLocalOf<State<IOSInsets>> {
    error("CompositionLocal LocalLayoutMarginsState not present")
}

/**
 * This class represents iOS Insets.
 * It contains equals and hashcode and can be used as Compose State<IOSInsets>.
 */
@Immutable
@InternalComposeApi
data class IOSInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp,
    val right: Dp = 0.dp,
)
