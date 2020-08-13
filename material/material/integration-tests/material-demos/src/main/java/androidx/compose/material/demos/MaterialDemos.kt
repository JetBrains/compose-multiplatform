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

import androidx.compose.material.samples.AlertDialogSample
import androidx.compose.material.samples.BottomDrawerSample
import androidx.compose.material.samples.CustomAlertDialogSample
import androidx.compose.material.samples.EmphasisSample
import androidx.compose.material.samples.ModalDrawerSample
import androidx.compose.material.samples.ScaffoldWithBottomBarAndCutout
import androidx.compose.material.samples.ScaffoldWithCoroutinesSnackbar
import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory

val MaterialDemos = DemoCategory("Material", listOf(
    DemoCategory("AlertDialog", listOf(
        ComposableDemo("Default dialog") { AlertDialogSample() },
        ComposableDemo("Custom buttons") { CustomAlertDialogSample() }
    )),
    ComposableDemo("App Bars") { AppBarDemo() },
    ComposableDemo("Bottom Navigation") { BottomNavigationDemo() },
    ComposableDemo("Buttons & FABs") { ButtonDemo() },
    DemoCategory("Navigation drawer", listOf(
        ComposableDemo("Modal drawer") { ModalDrawerSample() },
        ComposableDemo("Bottom drawer") { BottomDrawerSample() }
    )),
    ComposableDemo("Elevation") { ElevationDemo() },
    ComposableDemo("Emphasis") { EmphasisSample() },
    DemoCategory("ListItems", listOf(
        ComposableDemo("ListItems") { ListItemDemo() },
        ComposableDemo("Mixing RTL and LTR") { MixedRtlLtrListItemDemo() }
    )),
    ComposableDemo("Material Theme") { MaterialThemeDemo() },
    ComposableDemo("Menus") { MenuDemo() },
    DemoCategory(
        "Playground", listOf(
            ComposableDemo("Color Picker") { ColorPickerDemo() },
            ActivityDemo("Dynamic Theme", DynamicThemeActivity::class)
        )
    ),
    ComposableDemo("Progress Indicators") { ProgressIndicatorDemo() },
    ComposableDemo("Scaffold") { ScaffoldWithBottomBarAndCutout() },
    ComposableDemo("Selection Controls") { SelectionControlsDemo() },
    ComposableDemo("Slider") { SliderDemo() },
    ComposableDemo("Snackbar") { ScaffoldWithCoroutinesSnackbar() },
    ComposableDemo("Swipe to dismiss") { SwipeToDismissDemo() },
    ComposableDemo("Tabs") { TabDemo() },
    DemoCategory("TextFields", listOf(
        ComposableDemo("FilledTextField/OutlinedTextField") { MaterialTextFieldDemo() },
        ComposableDemo("Multiple text fields") { TextFieldsDemo() }
    ))
))
