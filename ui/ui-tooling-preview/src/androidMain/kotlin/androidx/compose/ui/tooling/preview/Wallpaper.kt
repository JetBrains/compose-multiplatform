/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.tooling.preview

import androidx.annotation.IntDef

/**
 * Wallpapers available to be used in the [Preview].
 */
object Wallpapers {
    /** Default value, representing dynamic theming not enabled. */
    const val NONE = -1
    /** Example wallpaper whose dominant colour is red. */
    const val RED_DOMINATED_EXAMPLE = 0
    /** Example wallpaper whose dominant colour is green. */
    const val GREEN_DOMINATED_EXAMPLE = 1
    /** Example wallpaper whose dominant colour is blue. */
    const val BLUE_DOMINATED_EXAMPLE = 2
    /** Example wallpaper whose dominant colour is yellow. */
    const val YELLOW_DOMINATED_EXAMPLE = 3
}

/**
 * Annotation for defining the wallpaper to use for dynamic theming in the [Preview].
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(Wallpapers.NONE, Wallpapers.RED_DOMINATED_EXAMPLE, Wallpapers.GREEN_DOMINATED_EXAMPLE,
    Wallpapers.BLUE_DOMINATED_EXAMPLE, Wallpapers.YELLOW_DOMINATED_EXAMPLE)
annotation class Wallpaper
