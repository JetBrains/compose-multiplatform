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

@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.layering

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/layering
 *
 * No action required if it's modified.
 */

@Composable
private fun LayeringSnippetControl1() {
    val color = animateColorAsState(if (condition) Color.Green else Color.Red)
}

@Composable
private fun LayeringSnippetControl2() {
    val color = remember { Animatable(Color.Gray) }
    LaunchedEffect(condition) {
        color.animateTo(if (condition) Color.Green else Color.Red)
    }
}

@Composable
private fun LayeringSnippetCustomization1() {
    @Composable
    fun Button(
        // …
        content: @Composable RowScope.() -> Unit
    ) {
        Surface(/* … */) {
            CompositionLocalProvider(/* LocalContentAlpha … */) {
                ProvideTextStyle(MaterialTheme.typography.button) {
                    Row(
                        // …
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun LayeringSnippetCustomization2() {
    @Composable
    fun GradientButton(
        // …
        background: List<Color>,
        content: @Composable RowScope.() -> Unit
    ) {
        Row(
            // …
            modifier = modifier
                .clickable(/* … */)
                .background(
                    Brush.horizontalGradient(background)
                )
        ) {
            // Use Material LocalContentAlpha & ProvideTextStyle
            CompositionLocalProvider(/* LocalContentAlpha … */) {
                ProvideTextStyle(MaterialTheme.typography.button) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun LayeringSnippetCustomization3() {
    @Composable
    fun BespokeButton(
        // …
        content: @Composable RowScope.() -> Unit
    ) {
        // No Material components used
        Row(
            // …
            modifier = modifier
                .clickable(/* … */)
                .background(/* … */),
            content = content
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private val condition = false
private val modifier = Modifier

private fun Modifier.clickable() = this
private fun Modifier.background() = this
