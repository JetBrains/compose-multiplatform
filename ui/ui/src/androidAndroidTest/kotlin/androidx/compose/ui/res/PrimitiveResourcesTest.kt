/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.res

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.R
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PrimitiveResourcesTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun integerResourceTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(integerResource(R.integer.integer_value)).isEqualTo(123)
            }
        }
    }

    @Test
    fun integerArrayResourceTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(integerArrayResource(R.array.integer_array))
                    .isEqualTo(intArrayOf(234, 345))
            }
        }
    }

    @Test
    fun boolArrayResourceTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(booleanResource(R.bool.boolean_value)).isTrue()
            }
        }
    }

    @Test
    fun dimensionResourceTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(dimensionResource(R.dimen.dimension_value)).isEqualTo(32.dp)
            }
        }
    }
}