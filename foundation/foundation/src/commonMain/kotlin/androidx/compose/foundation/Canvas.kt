/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Component that allow you to specify an area on the screen and perform canvas drawing on this
 * area. You MUST specify size with modifier, whether with exact sizes via [Modifier.size]
 * modifier, or relative to parent, via [Modifier.fillMaxSize], [ColumnScope.weight], etc. If parent
 * wraps this child, only exact sizes must be specified.
 *
 * @sample androidx.compose.foundation.samples.CanvasSample
 *
 * @param modifier mandatory modifier to specify size strategy for this composable
 * @param onDraw lambda that will be called to perform drawing. Note that this lambda will be
 * called during draw stage, you have no access to composition scope, meaning that [Composable]
 * function invocation inside it will result to runtime exception
 */
@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) =
    Spacer(modifier.drawBehind(onDraw))

/**
 * Component that allow you to specify an area on the screen and perform canvas drawing on this
 * area. You MUST specify size with modifier, whether with exact sizes via [Modifier.size]
 * modifier, or relative to parent, via [Modifier.fillMaxSize], [ColumnScope.weight], etc. If parent
 * wraps this child, only exact sizes must be specified.
 *
 * @sample androidx.compose.foundation.samples.CanvasPieChartSample
 *
 * @param modifier mandatory modifier to specify size strategy for this composable
 * @param contentDescription text used by accessibility services to describe what this canvas
 * represents. This should be provided unless the canvas is used for decorative purposes or as
 * part of a larger entity already described in some other way. This text should be localized,
 * such as by using [androidx.compose.ui.res.stringResource]
 * @param onDraw lambda that will be called to perform drawing. Note that this lambda will be
 * called during draw stage, you have no access to composition scope, meaning that [Composable]
 * function invocation inside it will result to runtime exception
 */
@ExperimentalFoundationApi
@Composable
fun Canvas(modifier: Modifier, contentDescription: String, onDraw: DrawScope.() -> Unit) =
    Spacer(modifier.drawBehind(onDraw).semantics { this.contentDescription = contentDescription })