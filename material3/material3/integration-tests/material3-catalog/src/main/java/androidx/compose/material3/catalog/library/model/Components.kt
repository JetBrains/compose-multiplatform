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

package androidx.compose.material3.catalog.library.model

import androidx.annotation.DrawableRes
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.util.ComponentGuidelinesUrl
import androidx.compose.material3.catalog.library.util.DocsUrl
import androidx.compose.material3.catalog.library.util.Material3SourceUrl
import androidx.compose.material3.catalog.library.util.PackageSummaryUrl
import androidx.compose.material3.catalog.library.util.StyleGuidelinesUrl

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

private var nextId: Int = 1
private fun nextId(): Int = nextId.also { nextId += 1 }

// Components are ordered alphabetically by name.

private val Buttons = Component(
    id = nextId(),
    name = "Buttons",
    description = "Buttons help people initiate actions, from sending an email, to sharing a " +
        "document, to liking a post.",
    // No buttons icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/buttons",
    docsUrl = "$PackageSummaryUrl#button",
    sourceUrl = "$Material3SourceUrl/Button.kt",
    examples = ButtonsExamples,
)

private val Color = Component(
    id = nextId(),
    name = "Color",
    description = "Color is used to express style and communicate meaning.",
    // No color icon
    tintIcon = true,
    guidelinesUrl = "$StyleGuidelinesUrl/color/overview",
    docsUrl = "$DocsUrl/ColorScheme",
    sourceUrl = "$Material3SourceUrl/ColorScheme.kt",
    examples = ColorExamples
)

private val Dialogs = Component(
    id = nextId(),
    name = "Dialogs",
    description = "Dialogs provide important prompts in a user flow. They can require an action, " +
        "communicate information, or help users accomplish a task.",
    // No dialogs icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/dialogs",
    docsUrl = "$PackageSummaryUrl#alertdialog",
    sourceUrl = "$Material3SourceUrl/AlertDialog.kt",
    examples = DialogExamples
)

private val ExtendedFloatingActionButton = Component(
    id = nextId(),
    name = "Extended FAB",
    description = "Extended FABs help people take primary actions. They're wider than FABs to " +
        "accommodate a text label and larger target area.",
    // No extended FAB icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/extended-fab",
    docsUrl = "$PackageSummaryUrl#extendedfloatingactionbutton",
    sourceUrl = "$Material3SourceUrl/FloatingActionButton.kt",
    examples = ExtendedFABExamples,
)

private val FloatingActionButtons = Component(
    id = nextId(),
    name = "Floating action buttons",
    description = "The FAB represents the most important action on a screen. It puts key actions " +
        "within reach.",
    // No FABs icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/floating-action-button",
    docsUrl = "$PackageSummaryUrl#floatingactionbutton",
    sourceUrl = "$Material3SourceUrl/FloatingActionButton.kt",
    examples = FloatingActionButtonsExamples,
)

private val NavigationBar = Component(
    id = nextId(),
    name = "Navigation bar",
    description = "Navigation bars offer a persistent and convenient way to switch between " +
        "primary destinations in an app.",
    // No navigation bar icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/navigation-bar",
    docsUrl = "$PackageSummaryUrl#navigationbar",
    sourceUrl = "$Material3SourceUrl/NavigationBar.kt",
    examples = NavigationBarExamples
)

private val NavigationDrawer = Component(
    id = nextId(),
    name = "Navigation drawer",
    description = "Navigation drawers provide ergonomic access to destinations in an app.",
    // No navigation drawer icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/navigation-drawer",
    docsUrl = "$PackageSummaryUrl#navigationdrawer",
    sourceUrl = "$Material3SourceUrl/NavigationDrawer.kt",
    examples = NavigationDrawerExamples
)

private val NavigationRail = Component(
    id = nextId(),
    name = "Navigation rail",
    description = "Navigation rails provide access to primary destinations in apps when using " +
        "tablet and desktop screens.",
    // No navigation rail icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/navigation-rail",
    docsUrl = "$PackageSummaryUrl#navigationrail",
    sourceUrl = "$Material3SourceUrl/NavigationRail.kt",
    examples = NavigationRailExamples
)

private val TopAppBar = Component(
    id = nextId(),
    name = "Top app bar",
    description = "Top app bars display information and actions at the top of a screen.",
    // No top app bar icon
    tintIcon = true,
    guidelinesUrl = "$ComponentGuidelinesUrl/top-app-bar",
    docsUrl = "$PackageSummaryUrl#smalltopappbar",
    sourceUrl = "$Material3SourceUrl/AppBar.kt",
    examples = TopAppBarExamples
)

/** Components for the catalog, ordered alphabetically by name. */
val Components = listOf(
    Buttons,
    Color,
    Dialogs,
    ExtendedFloatingActionButton,
    FloatingActionButtons,
    NavigationBar,
    NavigationDrawer,
    NavigationRail,
    TopAppBar
)
