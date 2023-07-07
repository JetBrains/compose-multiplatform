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

package androidx.compose.ui.window

/**
 * Describes how the window is placed on the screen.
 */
enum class WindowPlacement {
    /**
     * Window don't occupy the all available space and can be moved and resized by the user.
     */
    Floating,

    /**
     * The window is maximized and occupies all available space on the screen excluding
     * the space that is occupied by the screen insets (taskbar/dock and top-level application menu
     * on macOs).
     */
    Maximized,

    /**
     * The window is in fullscreen mode and occupies all available space of the screen,
     * including the space that is occupied by the screen insets (taskbar/dock and top-level
     * application menu on macOs).
     */
    Fullscreen
}