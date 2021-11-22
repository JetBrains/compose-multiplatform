// ktlint-disable filename
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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "ComposableNaming")

package androidx.compose.integration.docs.largescreens

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes
 *
 * No action required if it's modified.
 */

private object WindowSizeClassesSnippet {

    enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

    @Composable
    fun Activity.rememberWindowSizeClass() {
        val configuration = LocalConfiguration.current
        val windowMetrics = remember(configuration) {
            WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(this)
        }
        val windowDpSize = with(LocalDensity.current) {
            windowMetrics.bounds.toComposeRect().size.toDpSize()
        }
        val widthWindowSizeClass = when {
            windowDpSize.width < 600.dp -> WindowSizeClass.COMPACT
            windowDpSize.width < 840.dp -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        val heightWindowSizeClass = when {
            windowDpSize.height < 480.dp -> WindowSizeClass.COMPACT
            windowDpSize.height < 900.dp -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        // Use widthWindowSizeClass and heightWindowSizeClass
    }
}
