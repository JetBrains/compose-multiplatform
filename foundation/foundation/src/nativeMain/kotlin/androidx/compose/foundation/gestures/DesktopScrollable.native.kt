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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.gestures

import androidx.compose.ui.Modifier

// TODO(demin): implement smooth scroll animation on Windows
// TODO(demin): implement touchpad bounce physics on MacOS
// TODO(demin): maybe we need to differentiate different linux environments (Gnome/KDE)
// TODO(demin): do we need support real line scrolling (i.e. scroll by 3 text lines)?
internal actual fun Modifier.mouseScrollable(
    orientation: Orientation,
    onScroll: (Float) -> Unit
): Modifier = TODO("implement native Modifier.mouseScrollable")
