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

package androidx.compose.material.catalog.library.model

import androidx.annotation.DrawableRes
import androidx.compose.material.catalog.library.R
import androidx.compose.material.catalog.library.util.ComponentGuidelinesUrl
import androidx.compose.material.catalog.library.util.DocsUrl
import androidx.compose.material.catalog.library.util.MaterialSourceUrl

data class Component(
    val id: Int,
    val name: String,
    val description: String,
    @DrawableRes
    val icon: Int = R.drawable.ic_component,
    val tintIcon: Boolean = false,
    val guidelinesUrl: String,
    val docsUrl: String,
    val sourceUrl: String,
    val examples: List<Example>
)

private val AppBarsBottom = Component(
    id = 1,
    name = "App bars: bottom",
    description = "A bottom app bar displays navigation and key actions at the bottom of mobile " +
        "screens.",
    icon = R.drawable.ic_app_bars_bottom,
    guidelinesUrl = "$ComponentGuidelinesUrl/app-bars-bottom",
    docsUrl = "$DocsUrl#bottomappbar",
    sourceUrl = "$MaterialSourceUrl/AppBar.kt",
    examples = AppBarsBottomExamples
)

private val AppBarsTop = Component(
    id = 2,
    name = "App bars: top",
    description = "The top app bar displays information and actions relating to the current " +
        "screen.",
    icon = R.drawable.ic_app_bars_top,
    guidelinesUrl = "$ComponentGuidelinesUrl/app-bars-top",
    docsUrl = "$DocsUrl#topappbar",
    sourceUrl = "$MaterialSourceUrl/AppBar.kt",
    examples = AppBarsTopExamples
)

private val Backdrop = Component(
    id = 3,
    name = "Backdrop",
    description = "A backdrop appears behind all other surfaces in an app, displaying contextual " +
        "and actionable content.",
    // No backdrop icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/backdrop",
    docsUrl = "$DocsUrl#backdropscaffold",
    sourceUrl = "$MaterialSourceUrl/BackdropScaffold.kt",
    examples = BackdropExamples
)

private val Badge = Component(
    id = 22,
    name = "Badge",
    description = "A badge can contain dynamic information, such as the presence of a new " +
        "notification or a number of pending requests. Badges can be icon only or contain " +
        "a short text.",
    // No badge icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/bottom-navigation#behavior",
    docsUrl = "$DocsUrl#badgebox",
    sourceUrl = "$MaterialSourceUrl/Badge.kt",
    examples = BadgeExamples
)

private val NavigationRail = Component(
    id = 23,
    name = "Navigation Rail",
    description = "Navigation rails side navigation components allow movement between primary " +
        "destinations in an app.",
    icon = R.drawable.ic_navigation_rail,
    guidelinesUrl = "$ComponentGuidelinesUrl/navigation-rail",
    docsUrl = "$DocsUrl#navigationrail",
    sourceUrl = "$MaterialSourceUrl/NavigationRail.kt",
    examples = NavigationRailExamples
)

private val BottomNavigation = Component(
    id = 4,
    name = "Bottom navigation",
    description = "Bottom navigation bars allow movement between primary destinations in an app.",
    icon = R.drawable.ic_bottom_navigation,
    guidelinesUrl = "$ComponentGuidelinesUrl/bottom-navigation",
    docsUrl = "$DocsUrl#bottomnavigation",
    sourceUrl = "$MaterialSourceUrl/BottomNavigation.kt",
    examples = BottomNavigationExamples
)

private val Buttons = Component(
    id = 5,
    name = "Buttons",
    description = "Buttons allow users to take actions, and make choices, with a single tap.",
    icon = R.drawable.ic_buttons,
    guidelinesUrl = "$ComponentGuidelinesUrl/buttons",
    docsUrl = "$DocsUrl#backdropscaffold",
    sourceUrl = "$MaterialSourceUrl/BackdropScaffold.kt",
    examples = ButtonsExamples
)

private val ButtonsFloatingActionButton = Component(
    id = 6,
    name = "Floating action buttons",
    description = "A floating action button (FAB) represents the primary action of a screen.",
    icon = R.drawable.ic_buttons_floating_action_button,
    guidelinesUrl = "$ComponentGuidelinesUrl/buttons-floating-action-button",
    docsUrl = "$DocsUrl#floatingactionbutton",
    sourceUrl = "$MaterialSourceUrl/FloatingActionButton.kt",
    examples = ButtonsFloatingActionButtonExamples
)

private val Cards = Component(
    id = 7,
    name = "Cards",
    description = "Cards contain content and actions about a single subject.",
    icon = R.drawable.ic_cards,
    guidelinesUrl = "$ComponentGuidelinesUrl/cards",
    docsUrl = "$DocsUrl#card",
    sourceUrl = "$MaterialSourceUrl/Card.kt",
    examples = CardsExamples
)

private val Checkboxes = Component(
    id = 8,
    name = "Checkboxes",
    description = "Checkboxes allow the user to select one or more items from a set or turn an " +
        "option on or off.",
    icon = R.drawable.ic_checkboxes,
    guidelinesUrl = "$ComponentGuidelinesUrl/checkboxes",
    docsUrl = "$DocsUrl#checkbox",
    sourceUrl = "$MaterialSourceUrl/Checkbox.kt",
    examples = CheckboxesExamples
)

private val Chips = Component(
    id = 24,
    name = "Chips",
    description = "Chips allow users to enter information, make selections, filter content, or" +
        " trigger actions.",
    icon = R.drawable.ic_chips,
    guidelinesUrl = "$ComponentGuidelinesUrl/chips",
    docsUrl = "$DocsUrl#chip",
    sourceUrl = "$MaterialSourceUrl/Chip.kt",
    examples = ChipsExamples
)

private val Dialogs = Component(
    id = 9,
    name = "Dialogs",
    description = "Dialogs inform users about a task and can contain critical information, " +
        "require decisions, or involve multiple tasks.",
    icon = R.drawable.ic_dialogs,
    guidelinesUrl = "$ComponentGuidelinesUrl/dialogs",
    docsUrl = "$DocsUrl#alertdialog",
    sourceUrl = "$MaterialSourceUrl/AlertDialog.kt",
    examples = DialogsExamples
)

private val Dividers = Component(
    id = 10,
    name = "Dividers",
    description = "A divider is a thin line that groups content in lists and layouts.",
    // No dividers icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/dividers",
    docsUrl = "$DocsUrl#divider",
    sourceUrl = "$MaterialSourceUrl/Divider.kt",
    examples = DividersExamples
)

private val Lists = Component(
    id = 11,
    name = "Lists",
    description = "Lists are continuous, vertical indexes of text or images.",
    icon = R.drawable.ic_lists,
    guidelinesUrl = "$ComponentGuidelinesUrl/lists",
    docsUrl = "$DocsUrl#listitem",
    sourceUrl = "$MaterialSourceUrl/ListItem.kt",
    examples = ListsExamples
)

private val Menus = Component(
    id = 12,
    name = "Menus",
    description = "Menus display a list of choices on temporary surfaces.",
    icon = R.drawable.ic_menus,
    guidelinesUrl = "$ComponentGuidelinesUrl/menus",
    docsUrl = "$DocsUrl#dropdownmenu",
    sourceUrl = "$MaterialSourceUrl/Menu.kt",
    examples = MenusExamples
)

private val NavigationDrawer = Component(
    id = 13,
    name = "Navigation drawer",
    description = "Navigation drawers provide access to destinations in your app.",
    icon = R.drawable.ic_navigation_drawer,
    guidelinesUrl = "$ComponentGuidelinesUrl/navigation-drawer",
    docsUrl = "$DocsUrl#modaldrawer",
    sourceUrl = "$MaterialSourceUrl/Drawer.kt",
    examples = NavigationDrawerExamples
)

private val ProgressIndicators = Component(
    id = 14,
    name = "Progress indicators",
    description = "Progress indicators express an unspecified wait time or display the length of " +
        "a process.",
    icon = R.drawable.ic_progress_indicators,
    guidelinesUrl = "$ComponentGuidelinesUrl/progress-indicators",
    docsUrl = "$DocsUrl#circularprogressindicator",
    sourceUrl = "$MaterialSourceUrl/ProgressIndicator.kt",
    examples = ProgressIndicatorsExamples
)

private val RadioButtons = Component(
    id = 15,
    name = "Radio buttons",
    description = "Radio buttons allow the user to select one option from a set.",
    icon = R.drawable.ic_radio_buttons,
    guidelinesUrl = "$ComponentGuidelinesUrl/radio-buttons",
    docsUrl = "$DocsUrl#radiobutton",
    sourceUrl = "$MaterialSourceUrl/RadioButton.kt",
    examples = RadioButtonsExamples
)

private val SheetsBottom = Component(
    id = 16,
    name = "Sheets: bottom",
    description = "Bottom sheets are surfaces containing supplementary content that are anchored " +
        "to the bottom of the screen.",
    icon = R.drawable.ic_sheets_bottom,
    guidelinesUrl = "$ComponentGuidelinesUrl/sheets-bottom",
    docsUrl = "$DocsUrl#bottomsheetscaffold",
    sourceUrl = "$MaterialSourceUrl/BottomSheetScaffold.kt",
    examples = SheetsBottomExamples
)

private val Sliders = Component(
    id = 17,
    name = "Sliders",
    description = "Sliders allow users to make selections from a range of values.",
    icon = R.drawable.ic_sliders,
    guidelinesUrl = "$ComponentGuidelinesUrl/sliders",
    docsUrl = "$DocsUrl#slider",
    sourceUrl = "$MaterialSourceUrl/Slider.kt",
    examples = SlidersExamples
)

private val Snackbars = Component(
    id = 18,
    name = "Snackbars",
    description = "Snackbars provide brief messages about app processes at the bottom of the " +
        "screen.",
    icon = R.drawable.ic_snackbars,
    guidelinesUrl = "$ComponentGuidelinesUrl/snackbars",
    docsUrl = "$DocsUrl#snackbar",
    sourceUrl = "$MaterialSourceUrl/Snackbar.kt",
    examples = SnackbarsExamples
)

private val Switches = Component(
    id = 19,
    name = "Switches",
    description = "Switches toggle the state of a single setting on or off.",
    icon = R.drawable.ic_switches,
    guidelinesUrl = "$ComponentGuidelinesUrl/switches",
    docsUrl = "$DocsUrl#switch",
    sourceUrl = "$MaterialSourceUrl/Switch.kt",
    examples = SwitchesExamples
)

private val Tabs = Component(
    id = 20,
    name = "Tabs",
    description = "Tabs organize content across different screens, data sets, and other " +
        "interactions.",
    icon = R.drawable.ic_tabs,
    guidelinesUrl = "$ComponentGuidelinesUrl/tabs",
    docsUrl = "$DocsUrl#tab",
    sourceUrl = "$MaterialSourceUrl/Tab.kt",
    examples = TabsExamples
)

private val TextFields = Component(
    id = 21,
    name = "Text fields",
    description = "Text fields let users enter and edit text.",
    icon = R.drawable.ic_text_fields,
    guidelinesUrl = "$ComponentGuidelinesUrl/text-fields",
    docsUrl = "$DocsUrl#textfield",
    sourceUrl = "$MaterialSourceUrl/TextField.kt",
    examples = TextFieldsExamples
)

// Next id = 25

val Components = listOf(
    AppBarsBottom,
    AppBarsTop,
    Backdrop,
    Badge,
    BottomNavigation,
    NavigationRail,
    Buttons,
    ButtonsFloatingActionButton,
    Cards,
    Checkboxes,
    Chips,
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
