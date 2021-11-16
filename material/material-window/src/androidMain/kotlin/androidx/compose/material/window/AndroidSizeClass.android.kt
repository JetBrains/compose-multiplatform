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

package androidx.compose.material.window

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.window.layout.WindowMetricsCalculator

/**
 * Calculates [SizeClass] of the window.
 *
 * Whenever device configutation changes result in change of the width or height based
 * size classes, for example on device rotation or window resizing, this will return a new
 * [SizeClass] instance.
 */
@ExperimentalMaterialWindowApi
@Composable
fun Activity.rememberSizeClass(): SizeClass {
    // observe configuration changes and recalculate size class on corresponding changes
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    return remember(
        configuration.screenLayout,
        configuration.screenHeightDp,
        configuration.screenWidthDp,
        configuration.orientation,
        configuration.densityDpi,
        density.density
    ) {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
        SizeClass.calculateFromSize(size)
    }
}

/**
 * Calculates [WidthSizeClass] of the window.
 *
 * @see rememberSizeClass
 */
@ExperimentalMaterialWindowApi
@Composable
fun Activity.rememberWidthSizeClass(): WidthSizeClass = rememberSizeClass().width

/**
 * Calculates [HeightSizeClass] of the window.
 *
 * @see rememberSizeClass
 */
@ExperimentalMaterialWindowApi
@Composable
fun Activity.rememberHeightSizeClass(): HeightSizeClass = rememberSizeClass().height