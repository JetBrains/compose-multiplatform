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

package androidx.compose.ui.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RawDragGestureFilterComposeTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testInspectorValue() {
        val dragObserver = object : DragObserver {}
        val canStartDragging: () -> Boolean = { true }
        rule.setContent {
            val modifier = Modifier.rawDragGestureFilter(
                dragObserver,
                canStartDragging,
                Orientation.Horizontal
            ) as InspectableValue

            assertThat(modifier.nameFallback).isEqualTo("rawDragGestureFilter")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable()).containsExactly(
                ValueElement("dragObserver", dragObserver),
                ValueElement("canStartDragging", canStartDragging),
                ValueElement("orientation", Orientation.Horizontal)
            )
        }
    }
}
