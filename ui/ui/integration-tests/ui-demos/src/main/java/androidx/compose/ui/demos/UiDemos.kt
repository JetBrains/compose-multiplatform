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

package androidx.compose.ui.demos

import androidx.compose.foundation.demos.text.SoftwareKeyboardControllerDemo
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.demos.autofill.ExplicitAutofillTypesDemo
import androidx.compose.ui.demos.focus.AdjacentScrollablesFocusDemo
import androidx.compose.ui.demos.focus.CaptureFocusDemo
import androidx.compose.ui.demos.focus.ClickableInLazyColumnDemo
import androidx.compose.ui.demos.focus.ConditionalFocusabilityDemo
import androidx.compose.ui.demos.focus.CustomFocusOrderDemo
import androidx.compose.ui.demos.focus.FocusInDialogDemo
import androidx.compose.ui.demos.focus.FocusInPopupDemo
import androidx.compose.ui.demos.focus.FocusManagerMoveFocusDemo
import androidx.compose.ui.demos.focus.FocusableDemo
import androidx.compose.ui.demos.focus.OneDimensionalFocusSearchDemo
import androidx.compose.ui.demos.focus.ReuseFocusRequesterDemo
import androidx.compose.ui.demos.focus.ScrollableLazyRowFocusDemo
import androidx.compose.ui.demos.focus.ScrollableRowFocusDemo
import androidx.compose.ui.demos.focus.LazyListChildFocusDemos
import androidx.compose.ui.demos.focus.NestedLazyListFocusSearchDemo
import androidx.compose.ui.demos.focus.TwoDimensionalFocusSearchDemo
import androidx.compose.ui.demos.gestures.ButtonMetaStateDemo
import androidx.compose.ui.demos.gestures.DetectTapGesturesDemo
import androidx.compose.ui.demos.gestures.DoubleTapGestureFilterDemo
import androidx.compose.ui.demos.gestures.DoubleTapInTapDemo
import androidx.compose.ui.demos.gestures.DragAndScaleGestureFilterDemo
import androidx.compose.ui.demos.gestures.DragGestureFilterDemo
import androidx.compose.ui.demos.gestures.DragSlopExceededGestureFilterDemo
import androidx.compose.ui.demos.gestures.EventTypesDemo
import androidx.compose.ui.demos.gestures.HorizontalScrollersInVerticalScrollersDemo
import androidx.compose.ui.demos.gestures.LongPressDragGestureFilterDemo
import androidx.compose.ui.demos.gestures.LongPressGestureDetectorDemo
import androidx.compose.ui.demos.gestures.NestedLongPressDemo
import androidx.compose.ui.demos.gestures.NestedPressingDemo
import androidx.compose.ui.demos.gestures.NestedScrollDispatchDemo
import androidx.compose.ui.demos.gestures.NestedScrollingDemo
import androidx.compose.ui.demos.gestures.PointerInputDuringSubComp
import androidx.compose.ui.demos.gestures.PopupDragDemo
import androidx.compose.ui.demos.gestures.PressIndicatorGestureFilterDemo
import androidx.compose.ui.demos.gestures.RawDragGestureFilterDemo
import androidx.compose.ui.demos.gestures.ScaleGestureFilterDemo
import androidx.compose.ui.demos.gestures.ScrollGestureFilterDemo
import androidx.compose.ui.demos.gestures.VerticalScrollerInDrawerDemo
import androidx.compose.ui.demos.input.TouchModeDemo
import androidx.compose.ui.demos.keyinput.InterceptEnterToSendMessageDemo
import androidx.compose.ui.demos.keyinput.KeyInputDemo
import androidx.compose.ui.demos.modifier.CommunicatingModifierDemo
import androidx.compose.ui.demos.recyclerview.RecyclerViewDemos
import androidx.compose.ui.demos.viewinterop.AndroidInComposeDemos
import androidx.compose.ui.demos.viewinterop.ComplexTouchInterop
import androidx.compose.ui.demos.viewinterop.ComposeInAndroidCoordinatorLayout
import androidx.compose.ui.demos.viewinterop.ComposeInAndroidDemos
import androidx.compose.ui.demos.viewinterop.ComposeViewComposeNestedInterop
import androidx.compose.ui.demos.viewinterop.EditTextInteropDemo
import androidx.compose.ui.demos.viewinterop.FocusTransferDemo
import androidx.compose.ui.demos.viewinterop.NestedScrollInteropComposeParentWithAndroidChild
import androidx.compose.ui.demos.viewinterop.ViewComposeViewNestedScrollInteropDemo
import androidx.compose.ui.demos.viewinterop.ViewInteropDemo
import androidx.compose.ui.samples.NestedScrollConnectionSample

private val GestureDemos = DemoCategory(
    "Gestures",
    listOf(
        DemoCategory(
            "Common Gestures",
            listOf(
                ComposableDemo("Press Indication") { PressIndicatorGestureFilterDemo() },
                ComposableDemo("Tap") { DetectTapGesturesDemo() },
                ComposableDemo("Double Tap") { DoubleTapGestureFilterDemo() },
                ComposableDemo("Long Press") { LongPressGestureDetectorDemo() },
                ComposableDemo("Scroll") { ScrollGestureFilterDemo() },
                ComposableDemo("Drag") { DragGestureFilterDemo() },
                ComposableDemo("Long Press Drag") { LongPressDragGestureFilterDemo() },
                ComposableDemo("Scale") { ScaleGestureFilterDemo() },
                ComposableDemo("Button/Meta State") { ButtonMetaStateDemo() },
                ComposableDemo("Event Types") { EventTypesDemo() },
            )
        ),
        DemoCategory(
            "Building Block Gestures",
            listOf(
                ComposableDemo("Drag Slop Exceeded") { DragSlopExceededGestureFilterDemo() },
                ComposableDemo("Raw Drag") { RawDragGestureFilterDemo() }
            )
        ),
        DemoCategory(
            "Combinations / Case Studies",
            listOf(
                ComposableDemo("Nested Pressing") { NestedPressingDemo() },
                ComposableDemo("Horizontal Scrollers In Vertical Scroller") {
                    HorizontalScrollersInVerticalScrollersDemo()
                },
                ComposableDemo("Vertical Scroller in Nav Drawer") {
                    VerticalScrollerInDrawerDemo()
                },
                ComposableDemo("Nested Scrolling") { NestedScrollingDemo() },
                ComposableDemo("Drag and Scale") { DragAndScaleGestureFilterDemo() },
                ComposableDemo("Popup Drag") { PopupDragDemo() },
                ComposableDemo("Double Tap in Tap") { DoubleTapInTapDemo() },
                ComposableDemo("Nested Long Press") { NestedLongPressDemo() },
                ComposableDemo("Pointer Input During Sub Comp") { PointerInputDuringSubComp() }
            )
        ),
        DemoCategory(
            "New nested scroll",
            listOf(
                ComposableDemo("Nested scroll connection") { NestedScrollConnectionSample() },
                ComposableDemo("Nested scroll dispatch") { NestedScrollDispatchDemo() }
            )
        )
    )
)

private val FocusDemos = DemoCategory(
    "Focus",
    listOf(
        ComposableDemo("Focusable Siblings") { FocusableDemo() },
        ComposableDemo("Focus Within Dialog") { FocusInDialogDemo() },
        ComposableDemo("Focus Within Popup") { FocusInPopupDemo() },
        ComposableDemo("Reuse Focus Requester") { ReuseFocusRequesterDemo() },
        ComposableDemo("1D Focus Search") { OneDimensionalFocusSearchDemo() },
        ComposableDemo("2D Focus Search") { TwoDimensionalFocusSearchDemo() },
        ComposableDemo("Custom Focus Order") { CustomFocusOrderDemo() },
        ComposableDemo("FocusManager.moveFocus()") { FocusManagerMoveFocusDemo() },
        ComposableDemo("Capture/Free Focus") { CaptureFocusDemo() },
        ComposableDemo("Focus In Scrollable Row") { ScrollableRowFocusDemo() },
        ComposableDemo("Focus in Lazy Row") { ScrollableLazyRowFocusDemo() },
        ComposableDemo("LazyList Child Focusability") { LazyListChildFocusDemos() },
        ComposableDemo("Focus In Adjacent Scrollable Rows") { AdjacentScrollablesFocusDemo() },
        ComposableDemo("Clickable in LazyColumn") { ClickableInLazyColumnDemo() },
        ComposableDemo("Nested LazyLists") { NestedLazyListFocusSearchDemo() },
        ComposableDemo("Conditional Focusability") { ConditionalFocusabilityDemo() }
    )
)

private val KeyInputDemos = DemoCategory(
    "KeyInput",
    listOf(
        ComposableDemo("onKeyEvent") { KeyInputDemo() },
        ComposableDemo("onPreviewKeyEvent") { InterceptEnterToSendMessageDemo() },
    )
)

private val GraphicsDemos = DemoCategory(
    "Graphics",
    listOf(
        ComposableDemo("VectorGraphicsDemo") { VectorGraphicsDemo() },
        ComposableDemo("DeclarativeGraphicsDemo") { DeclarativeGraphicsDemo() }
    )
)

@OptIn(ExperimentalComposeUiApi::class)
private val NestedScrollInteropDemos = DemoCategory(
    "Nested Scroll Interop",
    listOf(
        ActivityDemo(
            "(Collaborating) View -> Compose",
            ComposeInAndroidCoordinatorLayout::class
        ),
        ActivityDemo(
            "(Collaborating) View -> Compose -> View",
            ViewComposeViewNestedScrollInteropDemo::class
        ),
        ComposableDemo("Compose -> View") {
            NestedScrollInteropComposeParentWithAndroidChild()
        },
        ComposableDemo("Compose -> (Collaborating) View -> Compose Interop") {
            ComposeViewComposeNestedInterop()
        }
    )
)

private val ViewInteropDemos = DemoCategory(
    "View Interop",
    listOf(
        ComposableDemo("Views interoperability") { ViewInteropDemo() },
        ComposeInAndroidDemos,
        AndroidInComposeDemos,
        ComplexTouchInterop,
        ComposableDemo("TextField Interop") { EditTextInteropDemo() },
        ComposableDemo("Focus Transfer") { FocusTransferDemo() },
        NestedScrollInteropDemos
    )
)

private val ModifierDemos = DemoCategory(
    "Modifiers",
    listOf(
        ComposableDemo("Inter-Modifier Communication") { CommunicatingModifierDemo() }
    )
)

val CoreDemos = DemoCategory(
    "Framework",
    listOf(
        ModifierDemos,
        ComposableDemo("Explicit autofill types") { ExplicitAutofillTypesDemo() },
        FocusDemos,
        KeyInputDemos,
        ComposableDemo("TouchMode") { TouchModeDemo() },
        ComposableDemo("Multiple collects measure") { MultipleCollectTest() },
        ComposableDemo("Dialog") { DialogDemo() },
        ComposableDemo("Popup") { PopupDemo() },
        GraphicsDemos,
        GestureDemos,
        ViewInteropDemos,
        ComposableDemo("Software Keyboard Controller") { SoftwareKeyboardControllerDemo() },
        RecyclerViewDemos
    )
)
