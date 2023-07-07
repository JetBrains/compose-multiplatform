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

package androidx.compose.material.catalog.library.model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.catalog.library.util.SampleSourceUrl
import androidx.compose.material.samples.AlertDialogSample
import androidx.compose.material.samples.BackdropScaffoldSample
import androidx.compose.material.samples.BottomDrawerSample
import androidx.compose.material.samples.BottomNavigationSample
import androidx.compose.material.samples.BottomNavigationItemWithBadge
import androidx.compose.material.samples.BottomNavigationWithOnlySelectedLabelsSample
import androidx.compose.material.samples.BottomSheetScaffoldSample
import androidx.compose.material.samples.ButtonSample
import androidx.compose.material.samples.ButtonWithIconSample
import androidx.compose.material.samples.CardSample
import androidx.compose.material.samples.CheckboxSample
import androidx.compose.material.samples.ChipGroupSingleLineSample
import androidx.compose.material.samples.ChipSample
import androidx.compose.material.samples.CircularProgressIndicatorSample
import androidx.compose.material.samples.ClickableCardSample
import androidx.compose.material.samples.ClickableListItems
import androidx.compose.material.samples.CompactNavigationRailSample
import androidx.compose.material.samples.CustomAlertDialogSample
import androidx.compose.material.samples.ExposedDropdownMenuSample
import androidx.compose.material.samples.FancyIndicatorContainerTabs
import androidx.compose.material.samples.FancyIndicatorTabs
import androidx.compose.material.samples.FancyTabs
import androidx.compose.material.samples.FluidExtendedFab
import androidx.compose.material.samples.IconTabs
import androidx.compose.material.samples.LeadingIconTabs
import androidx.compose.material.samples.LinearProgressIndicatorSample
import androidx.compose.material.samples.MenuSample
import androidx.compose.material.samples.ModalBottomSheetSample
import androidx.compose.material.samples.ModalDrawerSample
import androidx.compose.material.samples.NavigationRailBottomAlignSample
import androidx.compose.material.samples.NavigationRailSample
import androidx.compose.material.samples.NavigationRailWithOnlySelectedLabelsSample
import androidx.compose.material.samples.OneLineListItems
import androidx.compose.material.samples.OneLineRtlLtrListItems
import androidx.compose.material.samples.OutlinedButtonSample
import androidx.compose.material.samples.OutlinedChipWithIconSample
import androidx.compose.material.samples.OutlinedTextFieldSample
import androidx.compose.material.samples.PasswordTextField
import androidx.compose.material.samples.RadioButtonSample
import androidx.compose.material.samples.RadioGroupSample
import androidx.compose.material.samples.RangeSliderSample
import androidx.compose.material.samples.ScaffoldWithCoroutinesSnackbar
import androidx.compose.material.samples.ScaffoldWithCustomSnackbar
import androidx.compose.material.samples.ScaffoldWithSimpleSnackbar
import androidx.compose.material.samples.ScrollingFancyIndicatorContainerTabs
import androidx.compose.material.samples.ScrollingTextTabs
import androidx.compose.material.samples.SimpleBottomAppBar
import androidx.compose.material.samples.SimpleExtendedFabNoIcon
import androidx.compose.material.samples.SimpleExtendedFabWithIcon
import androidx.compose.material.samples.SimpleFab
import androidx.compose.material.samples.SimpleOutlinedTextFieldSample
import androidx.compose.material.samples.SimpleTextFieldSample
import androidx.compose.material.samples.SimpleTopAppBar
import androidx.compose.material.samples.SliderSample
import androidx.compose.material.samples.StepRangeSliderSample
import androidx.compose.material.samples.StepsSliderSample
import androidx.compose.material.samples.SwitchSample
import androidx.compose.material.samples.TextAndIconTabs
import androidx.compose.material.samples.TextArea
import androidx.compose.material.samples.TextButtonSample
import androidx.compose.material.samples.TextFieldSample
import androidx.compose.material.samples.TextFieldWithErrorState
import androidx.compose.material.samples.TextFieldWithHelperMessage
import androidx.compose.material.samples.TextFieldWithHideKeyboardOnImeAction
import androidx.compose.material.samples.TextFieldWithIcons
import androidx.compose.material.samples.TextFieldWithPlaceholder
import androidx.compose.material.samples.TextTabs
import androidx.compose.material.samples.ThreeLineListItems
import androidx.compose.material.samples.ThreeLineRtlLtrListItems
import androidx.compose.material.samples.TriStateCheckboxSample
import androidx.compose.material.samples.TwoLineListItems
import androidx.compose.material.samples.TwoLineRtlLtrListItems
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val content: @Composable () -> Unit
)

private const val AppBarsBottomExampleDescription = "App bars: bottom examples"
private const val AppBarsBottomExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val AppBarsBottomExamples = listOf(
    Example(
        name = ::SimpleBottomAppBar.name,
        description = AppBarsBottomExampleDescription,
        sourceUrl = AppBarsBottomExampleSourceUrl,
    ) {
        SimpleBottomAppBar()
    }
)

private const val AppBarsTopExampleDescription = "App bars: top examples"
private const val AppBarsTopExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val AppBarsTopExamples = listOf(
    Example(
        name = ::SimpleTopAppBar.name,
        description = AppBarsTopExampleDescription,
        sourceUrl = AppBarsTopExampleSourceUrl
    ) {
        SimpleTopAppBar()
    }
)

private const val BackdropExampleDescription = "Backdrop examples"
private const val BackdropExampleSourceUrl = "$SampleSourceUrl/BackdropScaffoldSamples.kt"
val BackdropExamples = listOf(
    Example(
        name = ::BackdropScaffoldSample.name,
        description = BackdropExampleDescription,
        sourceUrl = BackdropExampleSourceUrl
    ) {
        BackdropScaffoldSample()
    }
)

private const val BadgeExampleDescription = "Badge examples"
private const val BadgeExampleSourceUrl = "$SampleSourceUrl/BadgeSamples.kt"
val BadgeExamples = listOf(
    Example(
        name = ::BottomNavigationItemWithBadge.name,
        description = BadgeExampleDescription,
        sourceUrl = BadgeExampleSourceUrl
    ) {
        BottomNavigationItemWithBadge()
    }
)

private const val BottomNavigationExampleDescription = "Bottom navigation examples"
private const val BottomNavigationExampleSourceUrl = "$SampleSourceUrl/BottomNavigationSamples.kt"
val BottomNavigationExamples = listOf(
    Example(
        name = ::BottomNavigationSample.name,
        description = BottomNavigationExampleDescription,
        sourceUrl = BottomNavigationExampleSourceUrl
    ) {
        BottomNavigationSample()
    },
    Example(
        name = ::BottomNavigationWithOnlySelectedLabelsSample.name,
        description = BottomNavigationExampleDescription,
        sourceUrl = BottomNavigationExampleSourceUrl
    ) {
        BottomNavigationWithOnlySelectedLabelsSample()
    }
)

private const val ButtonsExampleDescription = "Buttons examples"
private const val ButtonsExampleSourceUrl = "$SampleSourceUrl/ButtonSamples.kt"
val ButtonsExamples = listOf(
    Example(
        name = ::ButtonSample.name,
        description = ButtonsExampleDescription,
        sourceUrl = ButtonsExampleSourceUrl
    ) {
        ButtonSample()
    },
    Example(
        name = ::OutlinedButtonSample.name,
        description = ButtonsExampleDescription,
        sourceUrl = ButtonsExampleSourceUrl
    ) {
        OutlinedButtonSample()
    },
    Example(
        name = ::TextButtonSample.name,
        description = ButtonsExampleDescription,
        sourceUrl = ButtonsExampleSourceUrl
    ) {
        TextButtonSample()
    },
    Example(
        name = ::ButtonWithIconSample.name,
        description = ButtonsExampleDescription,
        sourceUrl = ButtonsExampleSourceUrl
    ) {
        ButtonWithIconSample()
    }
)

private const val ButtonsFloatingActionButtonExampleDescription = "Floating action button examples"
private const val ButtonsFloatingActionButtonExampleSourceUrl = "$SampleSourceUrl/" +
    "FloatingActionButtonSamples.kt"
val ButtonsFloatingActionButtonExamples = listOf(
    Example(
        name = ::SimpleFab.name,
        description = ButtonsFloatingActionButtonExampleDescription,
        sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl
    ) {
        SimpleFab()
    },
    Example(
        name = ::SimpleExtendedFabNoIcon.name,
        description = ButtonsFloatingActionButtonExampleDescription,
        sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl
    ) {
        SimpleExtendedFabNoIcon()
    },
    Example(
        name = ::SimpleExtendedFabWithIcon.name,
        description = ButtonsFloatingActionButtonExampleDescription,
        sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl
    ) {
        SimpleExtendedFabWithIcon()
    },
    Example(
        name = ::FluidExtendedFab.name,
        description = ButtonsFloatingActionButtonExampleDescription,
        sourceUrl = ButtonsFloatingActionButtonExampleSourceUrl
    ) {
        FluidExtendedFab()
    },
)

private const val CardsExampleDescription = "Cards examples"
private const val CardsExampleSourceUrl = "$SampleSourceUrl/CardSamples.kt"
val CardsExamples = listOf(
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
    }
)

private const val CheckboxesExampleDescription = "Checkboxes examples"
private const val CheckboxesExampleSourceUrl = "$SampleSourceUrl/SelectionControlsSamples.kt"
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

private const val ChipsExampleDescription = "Chips examples"
private const val ChipsExampleSourceUrl = "$SampleSourceUrl/ChipSamples.kt"
val ChipsExamples = listOf(
    Example(
        name = ::ChipSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        ChipSample()
    },
    Example(
        name = ::OutlinedChipWithIconSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        OutlinedChipWithIconSample()
    },
    Example(
        name = ::ChipGroupSingleLineSample.name,
        description = ChipsExampleDescription,
        sourceUrl = ChipsExampleSourceUrl
    ) {
        ChipGroupSingleLineSample()
    }
)

private const val DialogsExampleDescription = "Dialogs examples"
private const val DialogsExampleSourceUrl = "$SampleSourceUrl/AlertDialogSample.kt"
val DialogsExamples = listOf(
    Example(
        name = ::AlertDialogSample.name,
        description = DialogsExampleDescription,
        sourceUrl = DialogsExampleSourceUrl
    ) {
        AlertDialogSample()
    },
    Example(
        name = ::CustomAlertDialogSample.name,
        description = DialogsExampleDescription,
        sourceUrl = DialogsExampleSourceUrl
    ) {
        CustomAlertDialogSample()
    }
)

// No divider samples
val DividersExamples = emptyList<Example>()

private const val ListsExampleDescription = "Lists examples"
private const val ListsExampleSourceUrl = "$SampleSourceUrl/ListSamples.kt"
val ListsExamples = listOf(
    Example(
        name = ::ClickableListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        ClickableListItems()
    },
    Example(
        name = ::OneLineListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        OneLineListItems()
    },
    Example(
        name = ::TwoLineListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        TwoLineListItems()
    },
    Example(
        name = ::ThreeLineListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        ThreeLineListItems()
    },
    Example(
        name = ::OneLineRtlLtrListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        OneLineRtlLtrListItems()
    },
    Example(
        name = ::TwoLineRtlLtrListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        TwoLineRtlLtrListItems()
    },
    Example(
        name = ::ThreeLineRtlLtrListItems.name,
        description = ListsExampleDescription,
        sourceUrl = ListsExampleSourceUrl
    ) {
        ThreeLineRtlLtrListItems()
    }
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
    }
)

private const val NavigationDrawerExampleDescription = "Navigation drawer examples"
private const val NavigationDrawerExampleSourceUrl = "$SampleSourceUrl/DrawerSamples.kt"
val NavigationDrawerExamples = listOf(
    Example(
        name = ::ModalDrawerSample.name,
        description = NavigationDrawerExampleDescription,
        sourceUrl = NavigationDrawerExampleSourceUrl
    ) {
        ModalDrawerSample()
    },
    Example(
        name = ::BottomDrawerSample.name,
        description = NavigationDrawerExampleDescription,
        sourceUrl = NavigationDrawerExampleSourceUrl
    ) {
        BottomDrawerSample()
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
private const val RadioButtonsExampleSourceUrl = "$SampleSourceUrl/SelectionControlsSamples.kt"
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

private const val SheetsBottomExampleDescription = "Sheets: bottom examples"
private const val SheetsBottomStandardExampleSourceUrl = "$SampleSourceUrl/" +
    "BottomSheetScaffoldSamples.kt"
private const val SheetsBottomModalExampleSourceUrl = "$SampleSourceUrl/" +
    "ModalBottomSheetSamples.kt"
val SheetsBottomExamples = listOf(
    Example(
        name = ::BottomSheetScaffoldSample.name,
        description = SheetsBottomExampleDescription,
        sourceUrl = SheetsBottomStandardExampleSourceUrl
    ) {
        BottomSheetScaffoldSample()
    },
    Example(
        name = ::ModalBottomSheetSample.name,
        description = SheetsBottomExampleDescription,
        sourceUrl = SheetsBottomModalExampleSourceUrl
    ) {
        ModalBottomSheetSample()
    }
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

private const val SwitchesExampleDescription = "Switches examples"
private const val SwitchesExampleSourceUrl = "$SampleSourceUrl/SelectionControlsSamples.kt"
val SwitchesExamples = listOf(
    Example(
        name = ::SwitchSample.name,
        description = SwitchesExampleDescription,
        sourceUrl = SwitchesExampleSourceUrl
    ) {
        SwitchSample()
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
        name = ::TextFieldWithErrorState.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithErrorState()
    },
    Example(
        name = ::TextFieldWithHelperMessage.name,
        description = TextFieldsExampleDescription,
        sourceUrl = TextFieldsExampleSourceUrl
    ) {
        TextFieldWithHelperMessage()
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
        Box(Modifier.wrapContentWidth().width(280.dp)) { it.content() }
    })
}

private const val NavigationRailExampleDescription = "Navigation Rail examples"
private const val NavigationRailExampleSourceUrl = "$SampleSourceUrl/NavigationRailSamples.kt"
val NavigationRailExamples = listOf(
    Example(
        name = ::NavigationRailSample.name,
        description = NavigationRailExampleDescription,
        sourceUrl = NavigationRailExampleSourceUrl
    ) {
        NavigationRailSample()
    },
    Example(
        name = ::NavigationRailWithOnlySelectedLabelsSample.name,
        description = NavigationRailExampleDescription,
        sourceUrl = NavigationRailExampleSourceUrl
    ) {
        NavigationRailWithOnlySelectedLabelsSample()
    },
    Example(
        name = ::CompactNavigationRailSample.name,
        description = NavigationRailExampleDescription,
        sourceUrl = NavigationRailExampleSourceUrl
    ) {
        CompactNavigationRailSample()
    },
    Example(
        name = ::NavigationRailBottomAlignSample.name,
        description = NavigationRailExampleDescription,
        sourceUrl = NavigationRailExampleSourceUrl
    ) {
        NavigationRailBottomAlignSample()
    }
)
