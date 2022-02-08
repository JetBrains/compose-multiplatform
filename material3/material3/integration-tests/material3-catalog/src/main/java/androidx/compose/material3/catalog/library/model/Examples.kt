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
import androidx.compose.material3.samples.CardSample
import androidx.compose.material3.samples.CheckboxSample
import androidx.compose.material3.samples.CircularProgressIndicatorSample
import androidx.compose.material3.samples.ClickableCardSample
import androidx.compose.material3.samples.ClickableElevatedCardSample
import androidx.compose.material3.samples.ClickableOutlinedCardSample
import androidx.compose.material3.samples.ColorSchemeSample
import androidx.compose.material3.samples.ElevatedButtonSample
import androidx.compose.material3.samples.ElevatedCardSample
import androidx.compose.material3.samples.EnterAlwaysSmallTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
import androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.FancyTabs
import androidx.compose.material3.samples.FancyIndicatorTabs
import androidx.compose.material3.samples.FancyIndicatorContainerTabs
import androidx.compose.material3.samples.FilledTonalButtonSample
import androidx.compose.material3.samples.FloatingActionButtonSample
import androidx.compose.material3.samples.IconTabs
import androidx.compose.material3.samples.LargeFloatingActionButtonSample
import androidx.compose.material3.samples.LinearProgressIndicatorSample
import androidx.compose.material3.samples.LeadingIconTabs
import androidx.compose.material3.samples.MenuSample
import androidx.compose.material3.samples.NavigationBarSample
import androidx.compose.material3.samples.NavigationBarWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.NavigationDrawerSample
import androidx.compose.material3.samples.NavigationRailBottomAlignSample
import androidx.compose.material3.samples.NavigationRailSample
import androidx.compose.material3.samples.NavigationRailWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.OutlinedButtonSample
import androidx.compose.material3.samples.OutlinedCardSample
import androidx.compose.material3.samples.PinnedSmallTopAppBar
import androidx.compose.material3.samples.RadioButtonSample
import androidx.compose.material3.samples.RadioGroupSample
import androidx.compose.material3.samples.ScaffoldWithCoroutinesSnackbar
import androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
import androidx.compose.material3.samples.ScaffoldWithIndefiniteSnackbar
import androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
import androidx.compose.material3.samples.ScrollingFancyIndicatorContainerTabs
import androidx.compose.material3.samples.ScrollingTextTabs
import androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
import androidx.compose.material3.samples.SimpleSmallTopAppBar
import androidx.compose.material3.samples.SliderSample
import androidx.compose.material3.samples.SmallFloatingActionButtonSample
import androidx.compose.material3.samples.StepsSliderSample
import androidx.compose.material3.samples.TextAndIconTabs
import androidx.compose.material3.samples.TextButtonSample
import androidx.compose.material3.samples.TextTabs
import androidx.compose.material3.samples.TriStateCheckboxSample
import androidx.compose.runtime.Composable

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val content: @Composable () -> Unit
)

private const val ButtonsExampleDescription = "Button examples"
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

private const val CardsExampleDescription = "Cards examples"
private const val CardsExampleSourceUrl = "$SampleSourceUrl/CardSamples.kt"
val CardExamples = listOf(
    Example(
        name = ::CardSample.name,
        description = CardsExampleDescription,
        sourceUrl = CardsExampleSourceUrl
    ) {
        CardSample()
    },
    Example(
        name = ::ClickableCardSample.name,
        description = CardsExampleDescription,
        sourceUrl = CardsExampleSourceUrl
    ) {
        ClickableCardSample()
    },
    Example(
        name = ::ElevatedCardSample.name,
        description = CardsExampleDescription,
        sourceUrl = CardsExampleSourceUrl
    ) {
        ElevatedCardSample()
    },
    Example(
        name = ::ClickableElevatedCardSample.name,
        description = CardsExampleDescription,
        sourceUrl = CardsExampleSourceUrl
    ) {
        ClickableElevatedCardSample()
    },
    Example(
        name = ::OutlinedCardSample.name,
        description = CardsExampleDescription,
        sourceUrl = CardsExampleSourceUrl
    ) {
        OutlinedCardSample()
    },
    Example(
        name = ::ClickableOutlinedCardSample.name,
        description = CardsExampleDescription,
        sourceUrl = CardsExampleSourceUrl
    ) {
        ClickableOutlinedCardSample()
    }
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

private const val CheckboxesExampleDescription = "Checkboxes examples"
private const val CheckboxesExampleSourceUrl = "$SampleSourceUrl/CheckboxSamples.kt"
val CheckboxesExamples = listOf(
    Example(
        name = ::CheckboxSample.name,
        description = CheckboxesExampleDescription,
        sourceUrl = CheckboxesExampleSourceUrl
    ) {
        CheckboxSample()
    },
    Example(
        name = ::TriStateCheckboxSample.name,
        description = CheckboxesExampleDescription,
        sourceUrl = CheckboxesExampleSourceUrl
    ) {
        TriStateCheckboxSample()
    }
)

private const val DialogExampleDescription = "Dialog examples"
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
            name = ::SimpleCenterAlignedTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { SimpleCenterAlignedTopAppBar() },
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

private const val ExtendedFABExampleDescription = "Extended FAB examples"
private const val ExtendedFABExampleSourceUrl = "$SampleSourceUrl/FloatingActionButtonSamples.kt"
val ExtendedFABExamples =
    listOf(
        Example(
            name = ::ExtendedFloatingActionButtonSample.name,
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) { ExtendedFloatingActionButtonSample() },
    )

private const val FloatingActionButtonsExampleDescription = "Floating action button examples"
private const val FloatingActionButtonsExampleSourceUrl =
    "$SampleSourceUrl/FloatingActionButtonSamples.kt"
val FloatingActionButtonsExamples =
    listOf(
        Example(
            name = ::FloatingActionButtonSample.name,
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
        ) { FloatingActionButtonSample() },
        Example(
            name = ::LargeFloatingActionButtonSample.name,
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
        ) { LargeFloatingActionButtonSample() },
        Example(
            name = ::SmallFloatingActionButtonSample.name,
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
        ) { SmallFloatingActionButtonSample() }
    )

private const val MenusExampleDescription = "Menus examples"
private const val MenusExampleSourceUrl = "$SampleSourceUrl/MenuSamples.kt"
val MenusExamples = listOf(
    Example(
        name = ::MenuSample.name,
        description = MenusExampleDescription,
        sourceUrl = MenusExampleSourceUrl
    ) {
        MenuSample()
    }
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

private const val NavigationRailExampleDescription = "Navigation rail examples"
private const val NavigationRailExampleSourceUrl = "$SampleSourceUrl/NavigationRailSamples.kt"
val NavigationRailExamples =
    listOf(
        Example(
            name = ::NavigationRailSample.name,
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) { NavigationRailSample() },
        Example(
            name = ::NavigationRailWithOnlySelectedLabelsSample.name,
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) { NavigationRailWithOnlySelectedLabelsSample() },
        Example(
            name = ::NavigationRailBottomAlignSample.name,
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) { NavigationRailBottomAlignSample() },
    )

private const val NavigationDrawerExampleDescription = "Navigation drawer examples"
private const val NavigationDrawerExampleSourceUrl = "$SampleSourceUrl/DrawerSamples.kt"
val NavigationDrawerExamples = listOf(
    Example(
        name = ::NavigationDrawerSample.name,
        description = NavigationDrawerExampleDescription,
        sourceUrl = NavigationDrawerExampleSourceUrl
    ) {
        NavigationDrawerSample()
    }
)

private const val ProgressIndicatorsExampleDescription = "Progress indicators examples"
private const val ProgressIndicatorsExampleSourceUrl = "$SampleSourceUrl/" +
    "ProgressIndicatorSamples.kt"
val ProgressIndicatorsExamples = listOf(
    Example(
        name = ::LinearProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription,
        sourceUrl = ProgressIndicatorsExampleSourceUrl
    ) {
        LinearProgressIndicatorSample()
    },
    Example(
        name = ::CircularProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription,
        sourceUrl = ProgressIndicatorsExampleSourceUrl
    ) {
        CircularProgressIndicatorSample()
    }
)

private const val RadioButtonsExampleDescription = "Radio buttons examples"
private const val RadioButtonsExampleSourceUrl = "$SampleSourceUrl/RadioButtonSamples.kt"
val RadioButtonsExamples = listOf(
    Example(
        name = ::RadioButtonSample.name,
        description = RadioButtonsExampleDescription,
        sourceUrl = RadioButtonsExampleSourceUrl
    ) {
        RadioButtonSample()
    },
    Example(
        name = ::RadioGroupSample.name,
        description = RadioButtonsExampleDescription,
        sourceUrl = RadioButtonsExampleSourceUrl
    ) {
        RadioGroupSample()
    },
)

private const val SlidersExampleDescription = "Sliders examples"
private const val SlidersExampleSourceUrl = "$SampleSourceUrl/SliderSample.kt"
val SlidersExamples = listOf(
    Example(
        name = ::SliderSample.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        SliderSample()
    },
    Example(
        name = ::StepsSliderSample.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        StepsSliderSample()
    },
)

private const val SnackbarsExampleDescription = "Snackbars examples"
private const val SnackbarsExampleSourceUrl = "$SampleSourceUrl/ScaffoldSamples.kt"
val SnackbarsExamples = listOf(
    Example(
        name = ::ScaffoldWithSimpleSnackbar.name,
        description = SnackbarsExampleDescription,
        sourceUrl = SnackbarsExampleSourceUrl
    ) {
        ScaffoldWithSimpleSnackbar()
    },
    Example(
        name = ::ScaffoldWithIndefiniteSnackbar.name,
        description = SnackbarsExampleDescription,
        sourceUrl = SnackbarsExampleSourceUrl
    ) {
        ScaffoldWithIndefiniteSnackbar()
    },
    Example(
        name = ::ScaffoldWithCustomSnackbar.name,
        description = SnackbarsExampleDescription,
        sourceUrl = SnackbarsExampleSourceUrl
    ) {
        ScaffoldWithCustomSnackbar()
    },
    Example(
        name = ::ScaffoldWithCoroutinesSnackbar.name,
        description = SnackbarsExampleDescription,
        sourceUrl = SnackbarsExampleSourceUrl
    ) {
        ScaffoldWithCoroutinesSnackbar()
    }
)

private const val TabsExampleDescription = "Tabs examples"
private const val TabsExampleSourceUrl = "$SampleSourceUrl/TabSamples.kt"
val TabsExamples = listOf(
    Example(
        name = ::TextTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        TextTabs()
    },
    Example(
        name = ::IconTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        IconTabs()
    },
    Example(
        name = ::TextAndIconTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        TextAndIconTabs()
    },
    Example(
        name = ::LeadingIconTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        LeadingIconTabs()
    },
    Example(
        name = ::ScrollingTextTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        ScrollingTextTabs()
    },
    Example(
        name = ::FancyTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        FancyTabs()
    },
    Example(
        name = ::FancyIndicatorTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        FancyIndicatorTabs()
    },
    Example(
        name = ::FancyIndicatorContainerTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        FancyIndicatorContainerTabs()
    },
    Example(
        name = ::ScrollingFancyIndicatorContainerTabs.name,
        description = TabsExampleDescription,
        sourceUrl = TabsExampleSourceUrl
    ) {
        ScrollingFancyIndicatorContainerTabs()
    }
)