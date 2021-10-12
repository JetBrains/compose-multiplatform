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
import androidx.compose.material3.catalog.library.util.Material3SourceUrl

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

private val Button = Component(
    id = nextId(),
    name = "Button",
    description = "Buttons allow users to take actions, and make choices, with a single tap.",
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/Button.kt",
    examples = ButtonsExamples,
)

private val Color = Component(
    id = nextId(),
    name = "Color",
    description = "Material You colors",
    // No color icon
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/ColorScheme.kt",
    examples = ColorExamples
)

private val Dialog = Component(
    id = nextId(),
    name = "Dialog",
    description = "Material 3 basic dialogs",
    // No dialog icon
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/AlertDialog.kt",
    examples = DialogExamples
)

private val FloatingActionButtons = Component(
    id = nextId(),
    name = "Floating action button",
    description = "A floating action button (FAB) represents the primary action of a screen.",
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/FloatingActionButton.kt",
    examples = FloatingActionButtonsExamples,
)

private val NavigationBar = Component(
    id = nextId(),
    name = "Navigation bar",
    description = "Material You navigation bar",
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/NavigationBar.kt",
    examples = NavigationBarExamples
)

private val NavigationRail = Component(
    id = nextId(),
    name = "Navigation rail",
    description = "Material You navigation rail",
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/NavigationRail.kt",
    examples = NavigationRailExamples
)

private val TopAppBar = Component(
    id = nextId(),
    name = "Top app bar",
    description = "Material You top app bar",
    // No color icon
    tintIcon = true,
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/AppBar.kt",
    examples = TopAppBarExamples
)

private val NavigationDrawer = Component(
    id = nextId(),
    name = "Navigation drawer",
    description = "Navigation drawers provide access to destinations in your app.",
    // No navigation drawer icon
    guidelinesUrl = "", // No guidelines yet
    docsUrl = "", // No docs yet
    sourceUrl = "$Material3SourceUrl/Drawer.kt",
    examples = NavigationDrawerExamples
)

/** Components for the catalog, ordered alphabetically by name. */
val Components = listOf(
    Button,
    Color,
    Dialog,
    FloatingActionButtons,
    NavigationBar,
    NavigationRail,
    NavigationDrawer,
    TopAppBar
)
