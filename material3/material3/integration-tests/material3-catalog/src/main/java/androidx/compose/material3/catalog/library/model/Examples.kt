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

@file:Suppress("COMPOSABLE_FUNCTION_REFERENCE")

package androidx.compose.material3.catalog.library.model

import androidx.compose.material3.catalog.library.util.SampleSourceUrl
import androidx.compose.material3.samples.AlertDialogSample
import androidx.compose.material3.samples.AlertDialogWithIconSample
import androidx.compose.material3.samples.ButtonSample
import androidx.compose.material3.samples.ButtonWithIconSample
import androidx.compose.material3.samples.ColorSchemeSample
import androidx.compose.material3.samples.EnterAlwaysSmallTopAppBar
import androidx.compose.material3.samples.ElevatedButtonSample
import androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
import androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.FilledTonalButtonSample
import androidx.compose.material3.samples.FloatingActionButtonSample
import androidx.compose.material3.samples.LargeFloatingActionButtonSample
import androidx.compose.material3.samples.NavigationBarSample
import androidx.compose.material3.samples.NavigationBarWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.OutlinedButtonSample
import androidx.compose.material3.samples.PinnedSmallTopAppBar
import androidx.compose.material3.samples.SimpleCenteredTopAppBar
import androidx.compose.material3.samples.SimpleSmallTopAppBar
import androidx.compose.material3.samples.SmallFloatingActionButtonSample
import androidx.compose.material3.samples.TextButtonSample
import androidx.compose.runtime.Composable

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val content: @Composable () -> Unit
)

private const val ButtonsExampleDescription = "Buttons examples"
private const val ButtonsExampleSourceUrl = "$SampleSourceUrl/ButtonSamples.kt"
val ButtonsExamples =
    listOf(
        Example(
            name = ::ButtonSample.name,
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) { ButtonSample() },
        Example(
            name = ::ElevatedButtonSample.name,
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) { ElevatedButtonSample() },
        Example(
            name = ::FilledTonalButtonSample.name,
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) { FilledTonalButtonSample() },
        Example(
            name = ::OutlinedButtonSample.name,
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) { OutlinedButtonSample() },
        Example(
            name = ::TextButtonSample.name,
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) { TextButtonSample() },
        Example(
            name = ::ButtonWithIconSample.name,
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) { ButtonWithIconSample() }
    )

private const val ColorExampleDescription = "Color examples"
private const val ColorExampleSourceUrl = "$SampleSourceUrl/ColorSamples.kt"
val ColorExamples =
    listOf(
        Example(
            name = ::ColorSchemeSample.name,
            description = ColorExampleDescription,
            sourceUrl = ColorExampleSourceUrl,
        ) { ColorSchemeSample() },
    )

private const val DialogExampleDescription = "Basic Dialog examples"
private const val DialogExampleSourceUrl = "$SampleSourceUrl/AlertDialogSamples.kt"
val DialogExamples =
    listOf(
        Example(
            name = ::AlertDialogSample.name,
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
        ) { AlertDialogSample() },
        Example(
            name = ::AlertDialogWithIconSample.name,
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
        ) { AlertDialogWithIconSample() },
    )

private const val TopAppBarExampleDescription = "Top app bar examples"
private const val TopAppBarExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val TopAppBarExamples =
    listOf(
        Example(
            name = ::SimpleSmallTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { SimpleSmallTopAppBar() },
        Example(
            name = ::SimpleCenteredTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { SimpleCenteredTopAppBar() },
        Example(
            name = ::PinnedSmallTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { PinnedSmallTopAppBar() },
        Example(
            name = ::EnterAlwaysSmallTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { EnterAlwaysSmallTopAppBar() },
        Example(
            name = ::ExitUntilCollapsedMediumTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { ExitUntilCollapsedMediumTopAppBar() },
        Example(
            name = ::ExitUntilCollapsedLargeTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { ExitUntilCollapsedLargeTopAppBar() },
    )

private const val ButtonsFloatingActionButtonExampleDescription =
    "Buttons: floating action button examples"
private const val ButtonsFloatingActionButtonExampleSourceUrl =
    "$SampleSourceUrl/FloatingActionButtonSamples.kt"
val FloatingActionButtonsExamples =
    listOf(
        Example(
            name = ::FloatingActionButtonSample.name,
            description = ButtonsFloatingActionButtonExampleDescription,
            sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl,
        ) { FloatingActionButtonSample() },
        Example(
            name = ::LargeFloatingActionButtonSample.name,
            description = ButtonsFloatingActionButtonExampleDescription,
            sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl,
        ) { LargeFloatingActionButtonSample() },
        Example(
            name = ::SmallFloatingActionButtonSample.name,
            description = ButtonsFloatingActionButtonExampleDescription,
            sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl,
        ) { SmallFloatingActionButtonSample() },
        Example(
            name = ::ExtendedFloatingActionButtonSample.name,
            description = ButtonsFloatingActionButtonExampleDescription,
            sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl,
        ) { ExtendedFloatingActionButtonSample() },
    )

private const val NavigationBarExampleDescription = "Navigation bar examples"
private const val NavigationBarExampleSourceUrl = "$SampleSourceUrl/NavigationBarSamples.kt"
val NavigationBarExamples =
    listOf(
        Example(
            name = ::NavigationBarSample.name,
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
        ) { NavigationBarSample() },
        Example(
            name = ::NavigationBarWithOnlySelectedLabelsSample.name,
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
        ) { NavigationBarWithOnlySelectedLabelsSample() },
    )