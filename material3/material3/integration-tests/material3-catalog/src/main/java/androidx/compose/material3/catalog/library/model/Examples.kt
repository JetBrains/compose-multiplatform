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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.catalog.library.util.SampleSourceUrl
import androidx.compose.material3.samples.AlertDialogSample
import androidx.compose.material3.samples.AlertDialogWithCustomContentSample
import androidx.compose.material3.samples.AlertDialogWithIconSample
import androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.AssistChipSample
import androidx.compose.material3.samples.BottomAppBarWithFAB
import androidx.compose.material3.samples.BottomSheetScaffoldNestedScrollSample
import androidx.compose.material3.samples.ModalBottomSheetSample
import androidx.compose.material3.samples.ButtonSample
import androidx.compose.material3.samples.ButtonWithIconSample
import androidx.compose.material3.samples.CardSample
import androidx.compose.material3.samples.CheckboxSample
import androidx.compose.material3.samples.CheckboxWithTextSample
import androidx.compose.material3.samples.ChipGroupSingleLineSample
import androidx.compose.material3.samples.CircularProgressIndicatorSample
import androidx.compose.material3.samples.ClickableCardSample
import androidx.compose.material3.samples.ClickableElevatedCardSample
import androidx.compose.material3.samples.ClickableOutlinedCardSample
import androidx.compose.material3.samples.DateInputSample
import androidx.compose.material3.samples.DatePickerDialogSample
import androidx.compose.material3.samples.DatePickerSample
import androidx.compose.material3.samples.DatePickerWithDateValidatorSample
import androidx.compose.material3.samples.DateRangePickerSample
import androidx.compose.material3.samples.DismissibleNavigationDrawerSample
import androidx.compose.material3.samples.DockedSearchBarSample
import androidx.compose.material3.samples.EditableExposedDropdownMenuSample
import androidx.compose.material3.samples.ElevatedAssistChipSample
import androidx.compose.material3.samples.ElevatedButtonSample
import androidx.compose.material3.samples.ElevatedCardSample
import androidx.compose.material3.samples.ElevatedFilterChipSample
import androidx.compose.material3.samples.ElevatedSuggestionChipSample
import androidx.compose.material3.samples.EnterAlwaysTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
import androidx.compose.material3.samples.ExposedDropdownMenuSample
import androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.FancyIndicatorContainerTabs
import androidx.compose.material3.samples.FancyIndicatorTabs
import androidx.compose.material3.samples.FancyTabs
import androidx.compose.material3.samples.FilledIconButtonSample
import androidx.compose.material3.samples.FilledIconToggleButtonSample
import androidx.compose.material3.samples.FilledTonalButtonSample
import androidx.compose.material3.samples.FilledTonalIconButtonSample
import androidx.compose.material3.samples.FilledTonalIconToggleButtonSample
import androidx.compose.material3.samples.FilterChipSample
import androidx.compose.material3.samples.FilterChipWithLeadingIconSample
import androidx.compose.material3.samples.FloatingActionButtonSample
import androidx.compose.material3.samples.IconButtonSample
import androidx.compose.material3.samples.IconTabs
import androidx.compose.material3.samples.IconToggleButtonSample
import androidx.compose.material3.samples.IndeterminateCircularProgressIndicatorSample
import androidx.compose.material3.samples.IndeterminateLinearProgressIndicatorSample
import androidx.compose.material3.samples.InputChipSample
import androidx.compose.material3.samples.InputChipWithAvatarSample
import androidx.compose.material3.samples.LargeFloatingActionButtonSample
import androidx.compose.material3.samples.LeadingIconTabs
import androidx.compose.material3.samples.LinearProgressIndicatorSample
import androidx.compose.material3.samples.MenuSample
import androidx.compose.material3.samples.ModalNavigationDrawerSample
import androidx.compose.material3.samples.NavigationBarItemWithBadge
import androidx.compose.material3.samples.NavigationBarSample
import androidx.compose.material3.samples.NavigationBarWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.NavigationRailBottomAlignSample
import androidx.compose.material3.samples.NavigationRailSample
import androidx.compose.material3.samples.NavigationRailWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.OneLineListItem
import androidx.compose.material3.samples.OutlinedButtonSample
import androidx.compose.material3.samples.OutlinedCardSample
import androidx.compose.material3.samples.OutlinedIconButtonSample
import androidx.compose.material3.samples.OutlinedIconToggleButtonSample
import androidx.compose.material3.samples.OutlinedTextFieldSample
import androidx.compose.material3.samples.PasswordTextField
import androidx.compose.material3.samples.PermanentNavigationDrawerSample
import androidx.compose.material3.samples.PinnedTopAppBar
import androidx.compose.material3.samples.PlainTooltipSample
import androidx.compose.material3.samples.PlainTooltipWithManualInvocationSample
import androidx.compose.material3.samples.RadioButtonSample
import androidx.compose.material3.samples.RadioGroupSample
import androidx.compose.material3.samples.RangeSliderSample
import androidx.compose.material3.samples.RangeSliderWithCustomComponents
import androidx.compose.material3.samples.RichTooltipSample
import androidx.compose.material3.samples.RichTooltipWithManualInvocationSample
import androidx.compose.material3.samples.ScaffoldWithCoroutinesSnackbar
import androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
import androidx.compose.material3.samples.ScaffoldWithIndefiniteSnackbar
import androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
import androidx.compose.material3.samples.ScrollingFancyIndicatorContainerTabs
import androidx.compose.material3.samples.ScrollingTextTabs
import androidx.compose.material3.samples.SearchBarSample
import androidx.compose.material3.samples.SimpleBottomAppBar
import androidx.compose.material3.samples.SimpleBottomSheetScaffoldSample
import androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
import androidx.compose.material3.samples.SimpleOutlinedTextFieldSample
import androidx.compose.material3.samples.SimpleTextFieldSample
import androidx.compose.material3.samples.SimpleTopAppBar
import androidx.compose.material3.samples.SliderSample
import androidx.compose.material3.samples.SliderWithCustomThumbSample
import androidx.compose.material3.samples.SliderWithCustomTrackAndThumb
import androidx.compose.material3.samples.SmallFloatingActionButtonSample
import androidx.compose.material3.samples.StepRangeSliderSample
import androidx.compose.material3.samples.StepsSliderSample
import androidx.compose.material3.samples.SuggestionChipSample
import androidx.compose.material3.samples.SwitchSample
import androidx.compose.material3.samples.SwitchWithThumbIconSample
import androidx.compose.material3.samples.TextAndIconTabs
import androidx.compose.material3.samples.TextArea
import androidx.compose.material3.samples.TextButtonSample
import androidx.compose.material3.samples.TextFieldSample
import androidx.compose.material3.samples.TextFieldWithErrorState
import androidx.compose.material3.samples.TextFieldWithHideKeyboardOnImeAction
import androidx.compose.material3.samples.TextFieldWithIcons
import androidx.compose.material3.samples.TextFieldWithPlaceholder
import androidx.compose.material3.samples.TextFieldWithPrefixAndSuffix
import androidx.compose.material3.samples.TextFieldWithSupportingText
import androidx.compose.material3.samples.TextTabs
import androidx.compose.material3.samples.ThreeLineListItem
import androidx.compose.material3.samples.TimeInputSample
import androidx.compose.material3.samples.TimePickerSample
import androidx.compose.material3.samples.TimePickerSwitchableSample
import androidx.compose.material3.samples.TriStateCheckboxSample
import androidx.compose.material3.samples.TwoLineListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val content: @Composable () -> Unit
)

private const val BadgeExampleDescription = "Badge examples"
private const val BadgeExampleSourceUrl = "$SampleSourceUrl/BadgeSamples.kt"
val BadgeExamples =
    listOf(
        Example(
            name = ::NavigationBarItemWithBadge.name,
            description = BadgeExampleDescription,
            sourceUrl = BadgeExampleSourceUrl
        ) { NavigationBarItemWithBadge() }
    )

private const val BottomSheetExampleDescription = "Bottom Sheet examples"
private const val BottomSheetExampleSourceUrl = "$SampleSourceUrl/BottomSheetSamples.kt"
val BottomSheetExamples =
    listOf(
        Example(
            name = ::ModalBottomSheetSample.name,
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl
        ) { ModalBottomSheetSample() },
        Example(
            name = ::SimpleBottomSheetScaffoldSample.name,
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl
        ) { SimpleBottomSheetScaffoldSample() },
        Example(
            name = ::BottomSheetScaffoldNestedScrollSample.name,
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl
        ) { BottomSheetScaffoldNestedScrollSample() }
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
        name = ::CheckboxWithTextSample.name,
        description = CheckboxesExampleDescription,
        sourceUrl = CheckboxesExampleSourceUrl
    ) {
        CheckboxWithTextSample()
    },
    Example(
        name = ::TriStateCheckboxSample.name,
        description = CheckboxesExampleDescription,
        sourceUrl = CheckboxesExampleSourceUrl
    ) {
        TriStateCheckboxSample()
    }
)

private const val ChipsExampleDescription = "Chips examples"
private const val ChipsExampleSourceUrl = "$SampleSourceUrl/ChipSamples.kt"
val ChipsExamples = listOf(
    Example(
        name = ::AssistChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        AssistChipSample()
    },
    Example(
        name = ::ElevatedAssistChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        ElevatedAssistChipSample()
    },
    Example(
        name = ::FilterChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        FilterChipSample()
    },
    Example(
        name = ::ElevatedFilterChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        ElevatedFilterChipSample()
    },
    Example(
        name = ::FilterChipWithLeadingIconSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        FilterChipWithLeadingIconSample()
    },
    Example(
        name = ::InputChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        InputChipSample()
    },
    Example(
        name = ::InputChipWithAvatarSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        InputChipWithAvatarSample()
    },
    Example(
        name = ::SuggestionChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        SuggestionChipSample()
    },
    Example(
        name = ::ElevatedSuggestionChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        ElevatedSuggestionChipSample()
    },
    Example(
        name = ::ChipGroupSingleLineSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        ChipGroupSingleLineSample()
    }
)

private const val DatePickerExampleDescription = "Date picker examples"
private const val DatePickerExampleSourceUrl = "$SampleSourceUrl/DatePickerSamples.kt"
val DatePickerExamples = listOf(
    Example(
        name = ::DatePickerSample.name,
        description = DatePickerExampleDescription,
        sourceUrl = DatePickerExampleSourceUrl
    ) {
        DatePickerSample()
    },
    Example(
        name = ::DatePickerDialogSample.name,
        description = DatePickerExampleDescription,
        sourceUrl = DatePickerExampleSourceUrl
    ) {
        DatePickerDialogSample()
    },
    Example(
        name = ::DatePickerWithDateValidatorSample.name,
        description = DatePickerExampleDescription,
        sourceUrl = DatePickerExampleSourceUrl
    ) {
        DatePickerWithDateValidatorSample()
    },
    Example(
        name = ::DateInputSample.name,
        description = DatePickerExampleDescription,
        sourceUrl = DatePickerExampleSourceUrl
    ) {
        DateInputSample()
    },
    Example(
        name = ::DateRangePickerSample.name,
        description = DatePickerExampleDescription,
        sourceUrl = DatePickerExampleSourceUrl
    ) {
        DateRangePickerSample()
    },
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
        Example(
            name = ::AlertDialogWithCustomContentSample.name,
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
        ) { AlertDialogWithCustomContentSample() },
    )

private const val BottomAppBarsExampleDescription = "Bottom app bar examples"
private const val BottomAppBarsExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val BottomAppBarsExamples = listOf(
    Example(
        name = ::SimpleBottomAppBar.name,
        description = BottomAppBarsExampleDescription,
        sourceUrl = BottomAppBarsExampleSourceUrl,
    ) { SimpleBottomAppBar() },
    Example(
        name = ::BottomAppBarWithFAB.name,
        description = BottomAppBarsExampleDescription,
        sourceUrl = BottomAppBarsExampleSourceUrl,
    ) { BottomAppBarWithFAB() }
)

private const val TopAppBarExampleDescription = "Top app bar examples"
private const val TopAppBarExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val TopAppBarExamples =
    listOf(
        Example(
            name = ::SimpleTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { SimpleTopAppBar() },
        Example(
            name = ::SimpleCenterAlignedTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { SimpleCenterAlignedTopAppBar() },
        Example(
            name = ::PinnedTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { PinnedTopAppBar() },
        Example(
            name = ::EnterAlwaysTopAppBar.name,
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) { EnterAlwaysTopAppBar() },
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
        Example(
            name = ::ExtendedFloatingActionButtonTextSample.name,
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) { ExtendedFloatingActionButtonTextSample() },
        Example(
            name = ::AnimatedExtendedFloatingActionButtonSample.name,
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) { AnimatedExtendedFloatingActionButtonSample() },
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

private const val ListsExampleDescription = "List examples"
private const val ListsExampleSourceUrl = "$SampleSourceUrl/ListSamples.kt"
val ListsExamples = listOf(
    Example(
        name = ::OneLineListItem.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        OneLineListItem()
    },
    Example(
        name = ::TwoLineListItem.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        TwoLineListItem()
    },
    Example(
        name = ::ThreeLineListItem.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        ThreeLineListItem()
    },
)

private const val IconButtonExampleDescription = "Icon button examples"
private const val IconButtonExampleSourceUrl = "$SampleSourceUrl/IconButtonSamples.kt"
val IconButtonExamples =
    listOf(
        Example(
            name = ::IconButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { IconButtonSample() },
        Example(
            name = ::IconToggleButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { IconToggleButtonSample() },
        Example(
            name = ::FilledIconButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { FilledIconButtonSample() },
        Example(
            name = ::FilledIconToggleButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { FilledIconToggleButtonSample() },
        Example(
            name = ::FilledTonalIconButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { FilledTonalIconButtonSample() },
        Example(
            name = ::FilledTonalIconToggleButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { FilledTonalIconToggleButtonSample() },
        Example(
            name = ::OutlinedIconButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { OutlinedIconButtonSample() },
        Example(
            name = ::OutlinedIconToggleButtonSample.name,
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) { OutlinedIconToggleButtonSample() }
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
    },
    Example(
        name = ::ExposedDropdownMenuSample.name,
        description = MenusExampleDescription,
        sourceUrl = MenusExampleSourceUrl
    ) {
        ExposedDropdownMenuSample()
    },
    Example(
        name = ::EditableExposedDropdownMenuSample.name,
        description = MenusExampleDescription,
        sourceUrl = MenusExampleSourceUrl
    ) {
        EditableExposedDropdownMenuSample()
    },
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
        name = ::ModalNavigationDrawerSample.name,
        description = NavigationDrawerExampleDescription,
        sourceUrl = NavigationDrawerExampleSourceUrl
    ) {
        ModalNavigationDrawerSample()
    },
    Example(
        name = ::PermanentNavigationDrawerSample.name,
        description = NavigationDrawerExampleDescription,
        sourceUrl = NavigationDrawerExampleSourceUrl
    ) {
        PermanentNavigationDrawerSample()
    },
    Example(
        name = ::DismissibleNavigationDrawerSample.name,
        description = NavigationDrawerExampleDescription,
        sourceUrl = NavigationDrawerExampleSourceUrl
    ) {
        DismissibleNavigationDrawerSample()
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
        name = ::IndeterminateLinearProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription,
        sourceUrl = ProgressIndicatorsExampleSourceUrl
    ) {
        IndeterminateLinearProgressIndicatorSample()
    },
    Example(
        name = ::CircularProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription,
        sourceUrl = ProgressIndicatorsExampleSourceUrl
    ) {
        CircularProgressIndicatorSample()
    },
    Example(
        name = ::IndeterminateCircularProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription,
        sourceUrl = ProgressIndicatorsExampleSourceUrl
    ) {
        IndeterminateCircularProgressIndicatorSample()
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

private const val SearchBarExampleDescription = "Search bar examples"
private const val SearchBarExampleSourceUrl = "$SampleSourceUrl/SearchBarSamples.kt"
val SearchBarExamples = listOf(
    Example(
        name = ::SearchBarSample.name,
        description = SearchBarExampleDescription,
        sourceUrl = SearchBarExampleSourceUrl
    ) {
        SearchBarSample()
    },
    Example(
        name = ::DockedSearchBarSample.name,
        description = SearchBarExampleDescription,
        sourceUrl = SearchBarExampleSourceUrl
    ) {
        DockedSearchBarSample()
    },
)

private const val SlidersExampleDescription = "Sliders examples"
private const val SlidersExampleSourceUrl = "$SampleSourceUrl/SliderSamples.kt"
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
    Example(
        name = ::SliderWithCustomThumbSample.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        SliderWithCustomThumbSample()
    },
    Example(
        name = ::SliderWithCustomTrackAndThumb.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        SliderWithCustomTrackAndThumb()
    },
    Example(
        name = ::RangeSliderSample.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        RangeSliderSample()
    },
    Example(
        name = ::StepRangeSliderSample.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        StepRangeSliderSample()
    },
    Example(
        name = ::RangeSliderWithCustomComponents.name,
        description = SlidersExampleDescription,
        sourceUrl = SlidersExampleSourceUrl
    ) {
        RangeSliderWithCustomComponents()
    }
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

private const val SwitchExampleDescription = "Switch examples"
private const val SwitchExampleSourceUrl = "$SampleSourceUrl/SwitchSamples.kt"
val SwitchExamples = listOf(
    Example(
        name = ::SwitchSample.name,
        description = SwitchExampleDescription,
        sourceUrl = SwitchExampleSourceUrl
    ) {
        SwitchSample()
    },

    Example(
        name = ::SwitchWithThumbIconSample.name,
        description = SwitchExampleDescription,
        sourceUrl = SwitchExampleSourceUrl
    ) {
        SwitchWithThumbIconSample()
    },
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

private const val TimePickerDescription = "Time Picker examples"
private const val TimePickerSourceUrl = "$SampleSourceUrl/TimePicker.kt"
val TimePickerExamples = listOf(
    Example(
        name = ::TimePickerSample.name,
        description = TimePickerDescription,
        sourceUrl = TimePickerSourceUrl
    ) {
        TimePickerSample()
    },
    Example(
        name = ::TimeInputSample.name,
        description = TimePickerDescription,
        sourceUrl = TimePickerSourceUrl
    ) {
        TimeInputSample()
    },
    Example(
        name = ::TimePickerSwitchableSample.name,
        description = TimePickerDescription,
        sourceUrl = TimePickerSourceUrl
    ) {
        TimePickerSwitchableSample()
    },
)

private const val TextFieldsExampleDescription = "Text fields examples"
private const val TextFieldsExampleSourceUrl = "$SampleSourceUrl/TextFieldSamples.kt"
val TextFieldsExamples = listOf(
    Example(
        name = ::SimpleTextFieldSample.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        SimpleTextFieldSample()
    },
    Example(
        name = ::TextFieldSample.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldSample()
    },
    Example(
        name = ::SimpleOutlinedTextFieldSample.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        SimpleOutlinedTextFieldSample()
    },
    Example(
        name = ::OutlinedTextFieldSample.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        OutlinedTextFieldSample()
    },
    Example(
        name = ::TextFieldWithIcons.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithIcons()
    },
    Example(
        name = ::TextFieldWithPlaceholder.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithPlaceholder()
    },
    Example(
        name = ::TextFieldWithPrefixAndSuffix.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithPrefixAndSuffix()
    },
    Example(
        name = ::TextFieldWithErrorState.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithErrorState()
    },
    Example(
        name = ::TextFieldWithSupportingText.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithSupportingText()
    },
    Example(
        name = ::PasswordTextField.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        PasswordTextField()
    },
    Example(
        name = ::TextFieldWithHideKeyboardOnImeAction.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithHideKeyboardOnImeAction()
    },
    Example(
        name = ::TextArea.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextArea()
    }
).map {
    // By default text field samples are minimal and don't have a `width` modifier to restrict the
    // width. As a result, they grow horizontally if enough text is typed. To prevent this behavior
    // in Catalog app the code below restricts the width of every text field sample
    it.copy(content = {
        Box(
            Modifier
                .wrapContentWidth()
                .width(280.dp)
        ) { it.content() }
    })
}

private const val TooltipsExampleDescription = "Tooltips examples"
private const val TooltipsExampleSourceUrl = "$SampleSourceUrl/TooltipSamples.kt"
val TooltipsExamples = listOf(
    Example(
        name = ::PlainTooltipSample.name,
        description = TooltipsExampleDescription,
        sourceUrl = TooltipsExampleSourceUrl
    ) {
        PlainTooltipSample()
    },
    Example(
        name = ::PlainTooltipWithManualInvocationSample.name,
        description = TooltipsExampleDescription,
        sourceUrl = TooltipsExampleSourceUrl
    ) {
        PlainTooltipWithManualInvocationSample()
    },
    Example(
        name = ::RichTooltipSample.name,
        description = TooltipsExampleDescription,
        sourceUrl = TooltipsExampleSourceUrl
    ) {
        RichTooltipSample()
    },
    Example(
        name = ::RichTooltipWithManualInvocationSample.name,
        description = TooltipsExampleDescription,
        sourceUrl = TooltipsExampleSourceUrl
    ) {
        RichTooltipWithManualInvocationSample()
    }
)
