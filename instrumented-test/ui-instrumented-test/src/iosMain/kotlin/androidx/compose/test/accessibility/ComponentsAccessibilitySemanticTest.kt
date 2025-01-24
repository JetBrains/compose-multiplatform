/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.ui.accessibility

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.test.utils.assertAccessibilityTree
import androidx.compose.test.utils.available
import androidx.compose.test.utils.findNodeWithTag
import androidx.compose.test.utils.runUIKitInstrumentedTest
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.*
import kotlin.test.*

class ComponentsAccessibilitySemanticTest {
    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun testProgressNodesSemantic() = runUIKitInstrumentedTest {
        var sliderValue = 0.4f
        setContentWithAccessibilityEnabled {
            Column {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it }
                )
                LinearProgressIndicator(progress = 0.7f)
                RangeSlider(
                    value = 30f..70f,
                    onValueChange = {},
                    valueRange = 0f..100f
                )
            }
        }

        assertAccessibilityTree {
            // Slider
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitAdjustable)
                value = "40%"
            }

            // LinearProgressIndicator
            node {
                isAccessibilityElement = true
                value = "70%"
                traits()
            }

            // Range Slider
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitAdjustable)
                value = "43%"
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitAdjustable)
                value = "57%"
            }
        }
    }

    @Test
    fun testSliderAction() = runUIKitInstrumentedTest {
        var sliderValue = 0.4f
        setContentWithAccessibilityEnabled {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier.testTag("Slider")
            )
        }

        var oldValue = sliderValue
        val sliderNode = findNodeWithTag("Slider")
        sliderNode.element?.accessibilityIncrement()
        assertTrue(oldValue < sliderValue)

        oldValue = sliderValue
        sliderNode.element?.accessibilityDecrement()
        assertTrue(oldValue > sliderValue)
    }

    @Test
    fun testToggleAndCheckboxSemantic() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column {
                Switch(false, {})
                Checkbox(false, {})
                TriStateCheckbox(ToggleableState.On, {})
                TriStateCheckbox(ToggleableState.Off, {})
                TriStateCheckbox(ToggleableState.Indeterminate, {})
            }
        }

        assertAccessibilityTree {
            // Switch
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
                if (available(iosMajorVersion = 17)) {
                    traits(UIAccessibilityTraitToggleButton)
                }
            }
            // Checkbox
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            // ToggleableState
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }
    }

    @Test
    fun testToggleAndCheckboxAction() = runUIKitInstrumentedTest {
        var switch by mutableStateOf(false)
        var checkbox by mutableStateOf(false)
        var triStateCheckbox by mutableStateOf(ToggleableState.Off)

        setContentWithAccessibilityEnabled {
            Column {
                Switch(
                    checked = switch,
                    onCheckedChange = { switch = it },
                    modifier = Modifier.testTag("Switch")
                )
                Checkbox(
                    checked = checkbox,
                    onCheckedChange = { checkbox = it },
                    modifier = Modifier.testTag("Checkbox")
                )
                TriStateCheckbox(
                    state = triStateCheckbox,
                    onClick = { triStateCheckbox = ToggleableState.On },
                    modifier = Modifier.testTag("TriStateCheckbox")
                )
            }
        }

        findNodeWithTag("Switch").element?.accessibilityActivate()
        assertTrue(switch)
        waitForIdle()
        findNodeWithTag("Switch").element?.accessibilityActivate()
        assertFalse(switch)

        findNodeWithTag("Checkbox").element?.accessibilityActivate()
        assertTrue(checkbox)
        waitForIdle()
        findNodeWithTag("Checkbox").element?.accessibilityActivate()
        assertFalse(checkbox)

        findNodeWithTag("TriStateCheckbox").element?.accessibilityActivate()
        assertEquals(ToggleableState.On, triStateCheckbox)
    }

    @Test
    fun testRadioButtonSelection() = runUIKitInstrumentedTest {
        var selectedIndex by mutableStateOf(0)

        setContentWithAccessibilityEnabled {
            Column {
                RadioButton(selected = selectedIndex == 0, onClick = { selectedIndex = 0 })
                RadioButton(selected = selectedIndex == 1, onClick = { selectedIndex = 1 })
                RadioButton(
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    Modifier.testTag("RadioButton")
                )
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }

        findNodeWithTag("RadioButton").element?.accessibilityActivate()
        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
        }

        selectedIndex = 0
        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }
    }

    @Test
    fun testImageSemantics() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column {
                Image(
                    ImageBitmap(10, 10),
                    contentDescription = null,
                    modifier = Modifier.testTag("Image 1")
                )
                Image(
                    ImageBitmap(10, 10),
                    contentDescription = null,
                    modifier = Modifier.testTag("Image 2").semantics { role = Role.Image }
                )
                Image(
                    ImageBitmap(10, 10),
                    contentDescription = "Abstract Picture",
                    modifier = Modifier.testTag("Image 3")
                )
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = false
                identifier = "Image 1"
                traits()
            }
            node {
                isAccessibilityElement = false
                identifier = "Image 2"
                traits(UIAccessibilityTraitImage)
            }
            node {
                isAccessibilityElement = true
                identifier = "Image 3"
                label = "Abstract Picture"
                traits(UIAccessibilityTraitImage)
            }
        }
    }

    @Test
    fun testTextSemantics() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column {
                Text("Static Text", modifier = Modifier.testTag("Text 1"))
                Text("Custom Button", modifier = Modifier.testTag("Text 2").clickable { })
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                identifier = "Text 1"
                label = "Static Text"
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                isAccessibilityElement = true
                identifier = "Text 2"
                label = "Custom Button"
                traits(UIAccessibilityTraitButton)
            }
        }
    }

    @Test
    fun testDisabledSemantics() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column {
                Button({}, enabled = false) {}
                TextField("", {}, enabled = false)
                Slider(value = 0f, onValueChange = {}, enabled = false)
                Switch(checked = false, onCheckedChange = {}, enabled = false)
                Checkbox(checked = false, onCheckedChange = {}, enabled = false)
                TriStateCheckbox(state = ToggleableState.Off, onClick = {}, enabled = false)
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitAdjustable,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                if (available(iosMajorVersion = 17)) {
                    traits(
                        UIAccessibilityTraitButton,
                        UIAccessibilityTraitToggleButton,
                        UIAccessibilityTraitNotEnabled
                    )
                } else {
                    traits(
                        UIAccessibilityTraitButton,
                        UIAccessibilityTraitNotEnabled
                    )
                }
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
        }
    }

    @Test
    fun testHeadingSemantics() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Scaffold(topBar = {
                TopAppBar {
                    Text("Header", modifier = Modifier.semantics { heading() })
                }
            }) {
                Column {
                    Text("Content")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Header"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitHeader)
            }
            node {
                label = "Content"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
        }
    }

    @Test
    fun testSelectionContainer() = runUIKitInstrumentedTest {
        @Composable
        fun LabeledInfo(label: String, data: String) {
            Text(
                buildAnnotatedString {
                    append("$label: ")
                    append(data)
                }
            )
        }

        setContentWithAccessibilityEnabled {
            SelectionContainer {
                Column {
                    Text("Title")
                    LabeledInfo("Subtitle", "subtitle")
                    LabeledInfo("Details", "details")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Title"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                label = "Subtitle: subtitle"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                label = "Details: details"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
        }
    }

    @Test
    fun testVisibleNodes() = runUIKitInstrumentedTest {
        var alpha by mutableStateOf(0f)

        setContentWithAccessibilityEnabled {
            Text("Hidden", modifier = Modifier.graphicsLayer {
                this.alpha = alpha
            })
        }

        assertAccessibilityTree {
            label = "Hidden"
            isAccessibilityElement = false
        }

        alpha = 1f
        assertAccessibilityTree {
            label = "Hidden"
            isAccessibilityElement = true
        }
    }

    @Test
    fun testVisibleNodeContainers() = runUIKitInstrumentedTest {
        var alpha by mutableStateOf(0f)

        setContentWithAccessibilityEnabled {
            Column {
                Text("Text 1")
                Row(modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                }) {
                    Text("Text 2")
                    Text("Text 3")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Text 1"
                isAccessibilityElement = true
            }
            node {
                label = "Text 2"
                isAccessibilityElement = false
            }
            node {
                label = "Text 3"
                isAccessibilityElement = false
            }
        }

        alpha = 1f
        assertAccessibilityTree {
            node {
                label = "Text 1"
                isAccessibilityElement = true
            }
            node {
                label = "Text 2"
                isAccessibilityElement = true
            }
            node {
                label = "Text 3"
                isAccessibilityElement = true
            }
        }
    }

    @Test
    fun testAccessibilityContainer() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column(modifier = Modifier.testTag("Container")) {
                Text("Text 1")
                Text("Text 2")
            }
        }

        assertAccessibilityTree {
            identifier = "Container"
            isAccessibilityElement = false
            node {
                label = "Text 1"
                isAccessibilityElement = true
            }
            node {
                label = "Text 2"
                isAccessibilityElement = true
            }
        }
    }

    @ExperimentalComposeUiApi
    @Test
    fun testAccessibilityInterop() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column(modifier = Modifier.testTag("Container")) {
                UIKitView(
                    factory = {
                        val view = UIView()
                        view.setIsAccessibilityElement(true)
                        view.setAccessibilityLabel("Disabled")
                        view
                    },
                    properties = UIKitInteropProperties(isNativeAccessibilityEnabled = false)
                )
                UIKitView(
                    factory = {
                        val view = UIView()
                        view.setIsAccessibilityElement(true)
                        view.setAccessibilityLabel("Enabled")
                        view
                    },
                    properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true)
                )
                UIKitView(
                    factory = {
                        val view = UIView()
                        view.setIsAccessibilityElement(true)
                        view.setAccessibilityLabel("Enabled With Tag")
                        view
                    },
                    properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
                    modifier = Modifier.testTag("Container Tag")
                )
            }
        }

        assertAccessibilityTree {
            identifier = "Container"
            isAccessibilityElement = false
            node {
                label = "Enabled"
                isAccessibilityElement = true
            }
            node {
                identifier = "Container Tag"
                isAccessibilityElement = false
                node {
                    label = "Enabled With Tag"
                    isAccessibilityElement = true
                }
            }
        }
    }

    @Test
    fun testChildrenOfCollapsedNode() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column {
                Row(modifier = Modifier.testTag("row").clickable {}) {
                    Text("Foo", modifier = Modifier.testTag("row_title"))
                    Text("Bar", modifier = Modifier.testTag("row_subtitle"))
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Foo\nBar"
                identifier = "row"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                label = "Foo"
                identifier = "row_title"
                isAccessibilityElement = false
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                label = "Bar"
                identifier = "row_subtitle"
                isAccessibilityElement = false
                traits(UIAccessibilityTraitStaticText)
            }
        }
    }
}
