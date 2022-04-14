/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.demos

import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.samples.AlertDialogSample
import androidx.compose.material.samples.BackdropScaffoldSample
import androidx.compose.material.samples.BottomDrawerSample
import androidx.compose.material.samples.CustomAlertDialogSample
import androidx.compose.material.samples.ModalBottomSheetSample
import androidx.compose.material.samples.ModalDrawerSample
import androidx.compose.material.samples.BottomSheetScaffoldSample
import androidx.compose.material.samples.ContentAlphaSample
import androidx.compose.material.samples.ScaffoldWithBottomBarAndCutout
import androidx.compose.material.samples.ScaffoldWithCoroutinesSnackbar
import androidx.compose.material.samples.ScaffoldWithSimpleSnackbar
import androidx.compose.material.samples.SimpleScaffoldWithTopBar

val MaterialDemos = DemoCategory(
    "Material",
    listOf(
        DemoCategory(
            "AlertDialog",
            listOf(
                ComposableDemo("Default dialog") { AlertDialogSample() },
                ComposableDemo("Custom buttons") { CustomAlertDialogSample() }
            )
        ),
        ComposableDemo("App Bars") { AppBarDemo() },
        ComposableDemo("Backdrop") { BackdropScaffoldSample() },
        ComposableDemo("Badge") { BadgeDemo() },
        ComposableDemo("Bottom Navigation") { BottomNavigationDemo() },
        DemoCategory(
            "Bottom Sheets",
            listOf(
                ComposableDemo("Bottom Sheet") { BottomSheetScaffoldSample() },
                ComposableDemo("Modal Bottom Sheet") { ModalBottomSheetSample() },
            )
        ),
        ComposableDemo("Buttons & FABs") { ButtonDemo() },
        ComposableDemo("Chips") { ChipDemo() },
        DemoCategory(
            "Navigation drawer",
            listOf(
                ComposableDemo("Modal drawer") { ModalDrawerSample() },
                ComposableDemo("Bottom drawer") { BottomDrawerSample() }
            )
        ),
        ComposableDemo("Elevation") { ElevationDemo() },
        ComposableDemo("Content alpha") { ContentAlphaSample() },
        DemoCategory(
            "ListItems",
            listOf(
                ComposableDemo("ListItems") { ListItemDemo() },
                ComposableDemo("Mixing RTL and LTR") { MixedRtlLtrListItemDemo() }
            )
        ),
        ComposableDemo("Material Theme") { MaterialThemeDemo() },
        DemoCategory(
            "Menus",
            listOf(
                ComposableDemo("Dropdown Menu positioning") { MenuDemo() },
                ComposableDemo("ExposedDropdownMenu") { ExposedDropdownMenuDemo() }
            )
        ),
        ComposableDemo("Navigation Rail") { NavigationRailDemo() },
        DemoCategory(
            "Playground",
            listOf(
                ComposableDemo("Color Picker") { ColorPickerDemo() },
                ActivityDemo("Dynamic Theme", DynamicThemeActivity::class)
            )
        ),
        ComposableDemo("Progress Indicators") { ProgressIndicatorDemo() },
        DemoCategory(
            "Scaffold",
            listOf(
                ComposableDemo("Scaffold with top bar") { SimpleScaffoldWithTopBar() },
                ComposableDemo("Scaffold with docked FAB") { ScaffoldWithBottomBarAndCutout() },
                ComposableDemo("Scaffold with snackbar") { ScaffoldWithSimpleSnackbar() }
            )
        ),
        ComposableDemo("Selection Controls") { SelectionControlsDemo() },
        ComposableDemo("Slider") { SliderDemo() },
        ComposableDemo("Snackbar") { ScaffoldWithCoroutinesSnackbar() },
        ComposableDemo("Swipe to dismiss") { SwipeToDismissDemo() },
        ComposableDemo("Tabs") { TabDemo() },
        DemoCategory(
            "TextFields",
            listOf(
                ComposableDemo("FilledTextField/OutlinedTextField") { MaterialTextFieldDemo() },
                ComposableDemo("Multiple text fields") { TextFieldsDemo() },
                ComposableDemo("Textfield decoration box") { DecorationBoxDemos() },
                ComposableDemo("Alignment inside text fields") { VerticalAlignmentsInTextField() }
            )
        )
    )
)
