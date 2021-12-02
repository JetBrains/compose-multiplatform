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

package androidx.compose.ui.platform

import androidx.compose.foundation.border
import androidx.compose.testutils.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.padding
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InspectableValueTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    fun Modifier.simple(padding: Int, border: Dp) = inspectable(
        debugInspectorInfo {
            name = "simple"
            properties["padding"] = padding
            properties["border"] = border
        }
    ) {
        padding(padding).border(border, color = Color.Blue)
    }

    @Test
    fun simpleLayoutTest() {
        val modifier = Modifier.simple(10, 3.dp).first() as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("simple")
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("padding", 10),
            ValueElement("border", 3.0.dp)
        )
    }
}
