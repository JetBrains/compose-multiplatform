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

package androidx.compose.material.catalog.model

import androidx.compose.material.samples.AlertDialogSample
import androidx.compose.material.samples.BackdropScaffoldSample
import androidx.compose.material.samples.BottomDrawerSample
import androidx.compose.material.samples.BottomNavigationSample
import androidx.compose.material.samples.BottomNavigationWithOnlySelectedLabelsSample
import androidx.compose.material.samples.BottomSheetScaffoldSample
import androidx.compose.material.samples.ButtonSample
import androidx.compose.material.samples.ButtonWithIconSample
import androidx.compose.material.samples.CardSample
import androidx.compose.material.samples.CheckboxSample
import androidx.compose.material.samples.CircularProgressIndicatorSample
import androidx.compose.material.samples.ClickableListItems
import androidx.compose.material.samples.CustomAlertDialogSample
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
import androidx.compose.material.samples.OneLineListItems
import androidx.compose.material.samples.OneLineRtlLtrListItems
import androidx.compose.material.samples.OutlinedButtonSample
import androidx.compose.material.samples.OutlinedTextFieldSample
import androidx.compose.material.samples.PasswordTextField
import androidx.compose.material.samples.RadioButtonSample
import androidx.compose.material.samples.RadioGroupSample
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
import androidx.compose.material.samples.StepsSliderSample
import androidx.compose.material.samples.SwitchSample
import androidx.compose.material.samples.TextAndIconTabs
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

data class Example(
    val name: String,
    val description: String,
    val content: @Composable () -> Unit
)

private const val AppBarsBottomExampleDescription = "App bars: bottom examples"
val AppBarsBottomExamples = listOf(
    Example(
        name = ::SimpleBottomAppBar.name,
        description = AppBarsBottomExampleDescription
    ) {
        SimpleBottomAppBar()
    }
)

private const val AppBarsTopExampleDescription = "App bars: top examples"
val AppBarsTopExamples = listOf(
    Example(
        name = ::SimpleTopAppBar.name,
        description = AppBarsTopExampleDescription
    ) {
        SimpleTopAppBar()
    }
)

private const val BackdropExampleDescription = "Backdrop examples"
val BackdropExamples = listOf(
    Example(
        name = ::BackdropScaffoldSample.name,
        description = BackdropExampleDescription
    ) {
        BackdropScaffoldSample()
    }
)

private const val BottomNavigationExampleDescription = "Bottom navigation examples"
val BottomNavigationExamples = listOf(
    Example(
        name = ::BottomNavigationSample.name,
        description = BottomNavigationExampleDescription
    ) {
        BottomNavigationSample()
    },
    Example(
        name = ::BottomNavigationWithOnlySelectedLabelsSample.name,
        description = BottomNavigationExampleDescription
    ) {
        BottomNavigationWithOnlySelectedLabelsSample()
    }
)

private const val ButtonsExampleDescription = "Buttons examples"
val ButtonsExamples = listOf(
    Example(
        name = ::ButtonSample.name,
        description = ButtonsExampleDescription
    ) {
        ButtonSample()
    },
    Example(
        name = ::OutlinedButtonSample.name,
        description = ButtonsExampleDescription
    ) {
        OutlinedButtonSample()
    },
    Example(
        name = ::TextButtonSample.name,
        description = ButtonsExampleDescription
    ) {
        TextButtonSample()
    },
    Example(
        name = ::ButtonWithIconSample.name,
        description = ButtonsExampleDescription
    ) {
        ButtonWithIconSample()
    }
)

private const val ButtonsFloatingActionButtonExampleDescription = "Buttons: floating action " +
    "button examples"
val ButtonsFloatingActionButtonExamples = listOf(
    Example(
        name = ::SimpleFab.name,
        description = ButtonsFloatingActionButtonExampleDescription
    ) {
        SimpleFab()
    },
    Example(
        name = ::SimpleExtendedFabNoIcon.name,
        description = ButtonsFloatingActionButtonExampleDescription
    ) {
        SimpleExtendedFabNoIcon()
    },
    Example(
        name = ::SimpleExtendedFabWithIcon.name,
        description = ButtonsFloatingActionButtonExampleDescription
    ) {
        SimpleExtendedFabWithIcon()
    },
    Example(
        name = ::FluidExtendedFab.name,
        description = ButtonsFloatingActionButtonExampleDescription
    ) {
        FluidExtendedFab()
    },
)

private const val CardsExampleDescription = "Cards examples"
val CardsExamples = listOf(
    Example(
        name = ::CardSample.name,
        description = CardsExampleDescription
    ) {
        CardSample()
    }
)

private const val CheckboxesExampleDescription = "Checkboxes examples"
val CheckboxesExamples = listOf(
    Example(
        name = ::CheckboxSample.name,
        description = CheckboxesExampleDescription
    ) {
        CheckboxSample()
    },
    Example(
        name = ::TriStateCheckboxSample.name,
        description = CheckboxesExampleDescription
    ) {
        TriStateCheckboxSample()
    }
)

private const val DialogsExampleDescription = "Dialogs examples"
val DialogsExamples = listOf(
    Example(
        name = ::AlertDialogSample.name,
        description = DialogsExampleDescription
    ) {
        AlertDialogSample()
    },
    Example(
        name = ::CustomAlertDialogSample.name,
        description = DialogsExampleDescription
    ) {
        CustomAlertDialogSample()
    }
)

// No divider samples
val DividersExamples = emptyList<Example>()

private const val ListsExampleDescription = "Lists examples"
val ListsExamples = listOf(
    Example(
        name = ::ClickableListItems.name,
        description = ListsExampleDescription
    ) {
        ClickableListItems()
    },
    Example(
        name = ::OneLineListItems.name,
        description = ListsExampleDescription
    ) {
        OneLineListItems()
    },
    Example(
        name = ::TwoLineListItems.name,
        description = ListsExampleDescription
    ) {
        TwoLineListItems()
    },
    Example(
        name = ::ThreeLineListItems.name,
        description = ListsExampleDescription
    ) {
        ThreeLineListItems()
    },
    Example(
        name = ::OneLineRtlLtrListItems.name,
        description = ListsExampleDescription
    ) {
        OneLineRtlLtrListItems()
    },
    Example(
        name = ::TwoLineRtlLtrListItems.name,
        description = ListsExampleDescription
    ) {
        TwoLineRtlLtrListItems()
    },
    Example(
        name = ::ThreeLineRtlLtrListItems.name,
        description = ListsExampleDescription
    ) {
        ThreeLineRtlLtrListItems()
    }
)

private const val MenusExampleDescription = "Menus examples"
val MenusExamples = listOf(
    Example(
        name = ::MenuSample.name,
        description = MenusExampleDescription
    ) {
        MenuSample()
    }
)

private const val NavigationDrawerExampleDescription = "Navigation drawer examples"
val NavigationDrawerExamples = listOf(
    Example(
        name = ::ModalDrawerSample.name,
        description = NavigationDrawerExampleDescription
    ) {
        ModalDrawerSample()
    },
    Example(
        name = ::BottomDrawerSample.name,
        description = NavigationDrawerExampleDescription
    ) {
        BottomDrawerSample()
    }
)

private const val ProgressIndicatorsExampleDescription = "Progress indicators examples"
val ProgressIndicatorsExamples = listOf(
    Example(
        name = ::LinearProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription
    ) {
        LinearProgressIndicatorSample()
    },
    Example(
        name = ::CircularProgressIndicatorSample.name,
        description = ProgressIndicatorsExampleDescription
    ) {
        CircularProgressIndicatorSample()
    }
)

private const val RadioButtonsExampleDescription = "Radio buttons examples"
val RadioButtonsExamples = listOf(
    Example(
        name = ::RadioButtonSample.name,
        description = RadioButtonsExampleDescription
    ) {
        RadioButtonSample()
    },
    Example(
        name = ::RadioGroupSample.name,
        description = RadioButtonsExampleDescription
    ) {
        RadioGroupSample()
    },
)

private const val SheetsBottomExampleDescription = "Sheets: bottom examples"
val SheetsBottomExamples = listOf(
    Example(
        name = ::BottomSheetScaffoldSample.name,
        description = SheetsBottomExampleDescription
    ) {
        BottomSheetScaffoldSample()
    },
    Example(
        name = ::ModalBottomSheetSample.name,
        description = SheetsBottomExampleDescription
    ) {
        ModalBottomSheetSample()
    }
)

private const val SlidersExampleDescription = "Sliders examples"
val SlidersExamples = listOf(
    Example(
        name = ::SliderSample.name,
        description = SlidersExampleDescription
    ) {
        SliderSample()
    },
    Example(
        name = ::StepsSliderSample.name,
        description = SlidersExampleDescription
    ) {
        StepsSliderSample()
    }
)

private const val SnackbarsExampleDescription = "Snackbars examples"
val SnackbarsExamples = listOf(
    Example(
        name = ::ScaffoldWithSimpleSnackbar.name,
        description = SnackbarsExampleDescription
    ) {
        ScaffoldWithSimpleSnackbar()
    },
    Example(
        name = ::ScaffoldWithCustomSnackbar.name,
        description = SnackbarsExampleDescription
    ) {
        ScaffoldWithCustomSnackbar()
    },
    Example(
        name = ::ScaffoldWithCoroutinesSnackbar.name,
        description = SnackbarsExampleDescription
    ) {
        ScaffoldWithCoroutinesSnackbar()
    }
)

private const val SwitchesExampleDescription = "Switches examples"
val SwitchesExamples = listOf(
    Example(
        name = ::SwitchSample.name,
        description = SwitchesExampleDescription
    ) {
        SwitchSample()
    }
)

private const val TabsExampleDescription = "Tabs examples"
val TabsExamples = listOf(
    Example(
        name = ::TextTabs.name,
        description = TabsExampleDescription
    ) {
        TextTabs()
    },
    Example(
        name = ::IconTabs.name,
        description = TabsExampleDescription
    ) {
        IconTabs()
    },
    Example(
        name = ::TextAndIconTabs.name,
        description = TabsExampleDescription
    ) {
        TextAndIconTabs()
    },
    Example(
        name = ::LeadingIconTabs.name,
        description = TabsExampleDescription
    ) {
        LeadingIconTabs()
    },
    Example(
        name = ::ScrollingTextTabs.name,
        description = TabsExampleDescription
    ) {
        ScrollingTextTabs()
    },
    Example(
        name = ::FancyTabs.name,
        description = TabsExampleDescription
    ) {
        FancyTabs()
    },
    Example(
        name = ::FancyIndicatorTabs.name,
        description = TabsExampleDescription
    ) {
        FancyIndicatorTabs()
    },
    Example(
        name = ::FancyIndicatorContainerTabs.name,
        description = TabsExampleDescription
    ) {
        FancyIndicatorContainerTabs()
    },
    Example(
        name = ::ScrollingFancyIndicatorContainerTabs.name,
        description = TabsExampleDescription
    ) {
        ScrollingFancyIndicatorContainerTabs()
    }
)

private const val TextFieldsExampleDescription = "Text fields examples"
val TextFieldsExamples = listOf(
    Example(
        name = ::SimpleTextFieldSample.name,
        description = TextFieldsExampleDescription
    ) {
        SimpleTextFieldSample()
    },
    Example(
        name = ::TextFieldSample.name,
        description = TextFieldsExampleDescription
    ) {
        TextFieldSample()
    },
    Example(
        name = ::SimpleOutlinedTextFieldSample.name,
        description = TextFieldsExampleDescription
    ) {
        SimpleOutlinedTextFieldSample()
    },
    Example(
        name = ::OutlinedTextFieldSample.name,
        description = TextFieldsExampleDescription
    ) {
        OutlinedTextFieldSample()
    },
    Example(
        name = ::TextFieldWithIcons.name,
        description = TextFieldsExampleDescription
    ) {
        TextFieldWithIcons()
    },
    Example(
        name = ::TextFieldWithPlaceholder.name,
        description = TextFieldsExampleDescription
    ) {
        TextFieldWithPlaceholder()
    },
    Example(
        name = ::TextFieldWithErrorState.name,
        description = TextFieldsExampleDescription
    ) {
        TextFieldWithErrorState()
    },
    Example(
        name = ::TextFieldWithHelperMessage.name,
        description = TextFieldsExampleDescription
    ) {
        TextFieldWithHelperMessage()
    },
    Example(
        name = ::PasswordTextField.name,
        description = TextFieldsExampleDescription
    ) {
        PasswordTextField()
    },
    Example(
        name = ::TextFieldWithHideKeyboardOnImeAction.name,
        description = TextFieldsExampleDescription
    ) {
        TextFieldWithHideKeyboardOnImeAction()
    }
)
