// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.integration.docs.layout.CustomLayoutsSnippet2.firstBaselineToTop
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/layouts/custom
 *
 * No action required if it's modified.
 */

private object CustomLayoutsSnippet1 {
    /* Can't be compiled without returning layout() from Modifier.layout. See next snippet for
    possible changes.

    fun Modifier.customLayoutModifier(/*...*/) =
        layout { measurable, constraints ->
            // ...
        }
    */
}

private object CustomLayoutsSnippet2 {
    fun Modifier.firstBaselineToTop(
        firstBaselineToTop: Dp
    ) = layout { measurable, constraints ->
        // Measure the composable
        val placeable = measurable.measure(constraints)

        // Check the composable has a first baseline
        check(placeable[FirstBaseline] != AlignmentLine.Unspecified)
        val firstBaseline = placeable[FirstBaseline]

        // Height of the composable with padding - first baseline
        val placeableY = firstBaselineToTop.roundToPx() - firstBaseline
        val height = placeable.height + placeableY
        layout(placeable.width, height) {
            // Where the composable gets placed
            placeable.placeRelative(0, placeableY)
        }
    }
}

private object CustomLayoutsSnippet3 {

    @Composable
    fun TextWithPaddingToBaselinePreview() {
        MyApplicationTheme {
            Text("Hi there!", Modifier.firstBaselineToTop(32.dp))
        }
    }

    //    @Preview
    @Composable
    fun TextWithNormalPaddingPreview() {
        MyApplicationTheme {
            Text("Hi there!", Modifier.padding(top = 32.dp))
        }
    }
}

private object CustomLayoutsSnippet4 {
    /* Can't be compiled without returning layout() from Layout. See previous snippet for possible
    changes.

    @Composable
    fun MyBasicColumn(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Layout(
            modifier = modifier,
            children = content
        ) { measurables, constraints ->
            // measure and position children given constraints logic here
        }
    }

     */
}

private object CustomLayoutsSnippet5and6 {
    @Composable
    fun MyBasicColumn(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Layout(
            modifier = modifier,
            content = content
        ) { measurables, constraints ->
            // Don't constrain child views further, measure them with given constraints
            // List of measured children
            val placeables = measurables.map { measurable ->
                // Measure each children
                measurable.measure(constraints)
            }

            // Set the size of the layout as big as it can
            layout(constraints.maxWidth, constraints.maxHeight) {
                // Track the y co-ord we have placed children up to
                var yPosition = 0

                // Place children in the parent layout
                placeables.forEach { placeable ->
                    // Position item on the screen
                    placeable.placeRelative(x = 0, y = yPosition)

                    // Record the y co-ord placed up to
                    yPosition += placeable.height
                }
            }
        }
    }

    // Snippet 6

    @Composable
    fun CallingComposable(modifier: Modifier = Modifier) {
        MyBasicColumn(modifier.padding(8.dp)) {
            Text("MyBasicColumn")
            Text("places items")
            Text("vertically.")
            Text("We've done it by hand!")
        }
    }
}

/*
Fakes needed for snippets to build:
 */

@Composable
private fun MyApplicationTheme(content: @Composable () -> Unit) {
}
