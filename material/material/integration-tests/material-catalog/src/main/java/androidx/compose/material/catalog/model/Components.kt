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

package androidx.compose.material.catalog.model

import androidx.annotation.DrawableRes
import androidx.compose.material.catalog.R

data class Component(
    val id: Int,
    val name: String,
    val description: String,
    @DrawableRes
    val icon: Int = R.drawable.ic_component,
    val examples: List<Example>
)

private val AppBarsBottom = Component(
    id = 1,
    name = "App bars: bottom",
    description = "A bottom app bar displays navigation and key actions at the bottom of mobile " +
        "screens.",
    examples = AppBarsBottomExamples
)

private val AppBarsTop = Component(
    id = 2,
    name = "App bars: top",
    description = "The top app bar displays information and actions relating to the current " +
        "screen.",
    examples = AppBarsTopExamples
)

private val Backdrop = Component(
    id = 3,
    name = "Backdrop",
    description = "A backdrop appears behind all other surfaces in an app, displaying contextual " +
        "and actionable content.",
    examples = BackdropExamples
)

private val BottomNavigation = Component(
    id = 4,
    name = "Bottom navigation",
    description = "Bottom navigation bars allow movement between primary destinations in an app.",
    examples = BottomNavigationExamples
)

private val Buttons = Component(
    id = 5,
    name = "Buttons",
    description = "Buttons allow users to take actions, and make choices, with a single tap.",
    examples = ButtonsExamples
)

private val ButtonsFloatingActionButton = Component(
    id = 6,
    name = "Buttons: floating action button",
    description = "A floating action button (FAB) represents the primary action of a screen.",
    examples = ButtonsFloatingActionButtonExamples
)

private val Cards = Component(
    id = 7,
    name = "Cards",
    description = "Cards contain content and actions about a single subject.",
    examples = CardsExamples
)

private val Checkboxes = Component(
    id = 8,
    name = "Checkboxes",
    description = "Checkboxes allow the user to select one or more items from a set or turn an " +
        "option on or off.",
    examples = CheckboxesExamples
)

private val Dialogs = Component(
    id = 9,
    name = "Dialogs",
    description = "Dialogs inform users about a task and can contain critical information, " +
        "require decisions, or involve multiple tasks.",
    examples = DialogsExamples
)

private val Dividers = Component(
    id = 10,
    name = "Dividers",
    description = "A divider is a thin line that groups content in lists and layouts.",
    examples = DividersExamples
)

private val Lists = Component(
    id = 11,
    name = "Lists",
    description = "Lists are continuous, vertical indexes of text or images.",
    examples = ListsExamples
)

private val Menus = Component(
    id = 12,
    name = "Menus",
    description = "Menus display a list of choices on temporary surfaces.",
    examples = MenusExamples
)

private val NavigationDrawer = Component(
    id = 13,
    name = "Navigation drawer",
    description = "Navigation drawers provide access to destinations in your app.",
    examples = NavigationDrawerExamples
)

private val ProgressIndicators = Component(
    id = 14,
    name = "Progress indicators",
    description = "Progress indicators express an unspecified wait time or display the length of " +
        "a process.",
    examples = ProgressIndicatorsExamples
)

private val RadioButtons = Component(
    id = 15,
    name = "Radio buttons",
    description = "Radio buttons allow the user to select one option from a set.",
    examples = RadioButtonsExamples
)

private val SheetsBottom = Component(
    id = 16,
    name = "Sheets: bottom",
    description = "Bottom sheets are surfaces containing supplementary content that are anchored " +
        "to the bottom of the screen.",
    examples = SheetsBottomExamples
)

private val Sliders = Component(
    id = 17,
    name = "Sliders",
    description = "Sliders allow users to make selections from a range of values.",
    examples = SlidersExamples
)

private val Snackbars = Component(
    id = 18,
    name = "Snackbars",
    description = "Snackbars provide brief messages about app processes at the bottom of the " +
        "screen.",
    examples = SnackbarsExamples
)

private val Switches = Component(
    id = 19,
    name = "Switches",
    description = "Switches toggle the state of a single setting on or off.",
    examples = SwitchesExamples
)

private val Tabs = Component(
    id = 20,
    name = "Tabs",
    description = "Tabs organize content across different screens, data sets, and other " +
        "interactions.",
    examples = TabsExamples
)

private val TextFields = Component(
    id = 21,
    name = "Text fields",
    description = "Text fields let users enter and edit text.",
    examples = TextFieldsExamples
)

val Components = listOf(
    AppBarsBottom,
    AppBarsTop,
    Backdrop,
    BottomNavigation,
    Buttons,
    ButtonsFloatingActionButton,
    Cards,
    Checkboxes,
    Dialogs,
    Dividers,
    Lists,
    Menus,
    NavigationDrawer,
    ProgressIndicators,
    RadioButtons,
    SheetsBottom,
    Sliders,
    Snackbars,
    Switches,
    Tabs,
    TextFields
)
