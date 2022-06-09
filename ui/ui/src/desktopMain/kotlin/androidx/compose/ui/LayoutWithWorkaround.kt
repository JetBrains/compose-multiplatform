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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints

/*
 * TODO: This is a version of Layout not using SAM-converted [MeasurePolicy] interface.
 * Remove me, when SAM conversion for [MeasurePolicy] works again in Kotlin 1.7.0
 * Check with Desktop jvm test DesktopAlertDialogTest.alignedToCenter_inPureWindow
 */
@Composable
internal inline fun LayoutWithWorkaround(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    crossinline measurePolicy: MeasureScope.(List<Measurable>, Constraints) -> MeasureResult
) = Layout(content, modifier) { x, y -> measurePolicy(x, y) }
