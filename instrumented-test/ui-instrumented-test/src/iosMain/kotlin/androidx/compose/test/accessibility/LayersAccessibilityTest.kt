/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.ui.accessibility

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.test.utils.assertAccessibilityTree
import androidx.compose.test.utils.runUIKitInstrumentedTest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.test.Test

class LayersAccessibilityTest {

    @Test
    fun testNodesCoveredByPopup() = runUIKitInstrumentedTest {
        val topPopup = mutableStateOf(false)
        val bottomPopup = mutableStateOf(false)
        val topPopupFocusable = mutableStateOf(false)
        setContentWithAccessibilityEnabled {
            Text("Root")
            if (bottomPopup.value) {
                Popup {
                    Text("Popup 1")
                }
            }
            if (topPopup.value) {
                Popup(properties = PopupProperties(focusable = topPopupFocusable.value)) {
                    Text("Popup 2")
                }
            }
        }

        assertAccessibilityTree {
            label = "Root"
        }

        bottomPopup.value = true
        // Non-focusable popup should not hide content under it for accessibility reader
        assertAccessibilityTree {
            node {
                label = "Root"
            }
            node {
                label = "Popup 1"
            }
        }

        topPopup.value = true
        // Non-focusable popup should not hide content under it for accessibility reader
        assertAccessibilityTree {
            node {
                label = "Root"
            }
            node {
                node {
                    label = "Popup 1"
                }
                node {
                    label = "Popup 2"
                }
            }
        }

        topPopupFocusable.value = true
        // Popup should react on focusable flag change
        assertAccessibilityTree {
            label = "Popup 2"
        }

        topPopup.value = false
        bottomPopup.value = false
        assertAccessibilityTree {
            label = "Root"
        }
    }

    @Test
    fun testNodesCoveredByDialog() = runUIKitInstrumentedTest {
        val showDialog = mutableStateOf(false)
        setContentWithAccessibilityEnabled {
            Text("Root")
            Popup {
                Text("Popup")
            }
            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    Text("Dialog")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Root"
            }
            node {
                label = "Popup"
            }
        }

        showDialog.value = true
        // Dialog popup should hide content under it for accessibility reader
        assertAccessibilityTree {
            label = "Dialog"
        }

        showDialog.value = false

        assertAccessibilityTree {
            node {
                label = "Root"
            }
            node {
                label = "Popup"
            }
        }
    }

    @Test
    fun testLayersAppearanceOrder() = runUIKitInstrumentedTest {
        val bottomLayer = mutableStateOf(false)
        val middleLayers = mutableStateOf(false)
        setContentWithAccessibilityEnabled {
            Text("Root")
            if (bottomLayer.value) {
                Popup(properties = PopupProperties(focusable = true)) {
                    Text("Bottom")
                }
            }
            if (middleLayers.value) {
                Popup(properties = PopupProperties(focusable = true)) {
                    Text("Middle 1")
                }
                // Non-focusable layer
                Popup {
                    Text("Middle 2")
                }
            }
            Popup(properties = PopupProperties(focusable = true)) {
                Text("Top")
            }
        }

        assertAccessibilityTree {
            label = "Top"
        }

        bottomLayer.value = true
        // The last added layer should be on top
        assertAccessibilityTree {
            label = "Bottom"
        }

        middleLayers.value = true
        // The last added layers should be on top
        assertAccessibilityTree {
            node {
                label = "Middle 1"
            }
            node {
                label = "Middle 2"
            }
        }
    }
}