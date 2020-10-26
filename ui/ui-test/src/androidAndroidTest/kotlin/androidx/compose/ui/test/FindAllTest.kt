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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class FindAllTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun findAllTest_twoComponents_areChecked() {
        rule.setContent {
            MaterialTheme {
                Surface {
                    Column {
                        Checkbox(checked = true, onCheckedChange = {}, enabled = false)
                        Checkbox(checked = true, onCheckedChange = {}, enabled = false)
                    }
                }
            }
        }

        rule.onAllNodes(isOn())
            .assertCountEquals(2)
            .apply {
                get(0).assertIsOn()
                get(1).assertIsOn()
            }
    }

    @Test
    fun findAllTest_twoComponents_toggleBoth() {
        rule.setContent {
            val (checked1, onCheckedChange1) = remember { mutableStateOf(false) }
            val (checked2, onCheckedChange2) = remember { mutableStateOf(false) }
            MaterialTheme {
                Surface {
                    Column {
                        Checkbox(
                            checked = checked1,
                            onCheckedChange = onCheckedChange1
                        )
                        Checkbox(
                            checked = checked2,
                            onCheckedChange = onCheckedChange2
                        )
                    }
                }
            }
        }

        rule.onAllNodes(isToggleable())
            .assertCountEquals(2)
            .apply {
                get(0)
                    .performClick()
                    .assertIsOn()
                get(1)
                    .performClick()
                    .assertIsOn()
            }
    }

    @Test
    fun findAllTest_noNonCheckedComponent() {
        rule.setContent {
            MaterialTheme {
                Surface {
                    Column {
                        Checkbox(checked = true, onCheckedChange = {}, enabled = false)
                        Checkbox(checked = true, onCheckedChange = {}, enabled = false)
                    }
                }
            }
        }

        rule.onAllNodes(isOff())
            .assertCountEquals(0)
    }

    @Test
    fun findAllTest_twoComponents_toggleOne() {
        rule.setContent {
            MaterialTheme {
                Surface {
                    val (checked1, onCheckedChange1) = remember { mutableStateOf(false) }
                    val (checked2, onCheckedChange2) = remember { mutableStateOf(false) }

                    Column {
                        Checkbox(
                            checked = checked1,
                            onCheckedChange = onCheckedChange1
                        )
                        Checkbox(
                            checked = checked2,
                            onCheckedChange = onCheckedChange2
                        )
                    }
                }
            }
        }

        rule.onAllNodes(isToggleable()).apply {
            get(0)
                .performClick()
                .assertIsOn()
            get(1)
                .assertIsOff()
        }.assertCountEquals(2)
    }

    @Test
    fun findAllTest_twoComponents_togglesCreatesAnother() {
        rule.setContent {
            MaterialTheme {
                Surface {
                    val (checked, onCheckedChange) = remember { mutableStateOf(false) }

                    Column {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                onCheckedChange(it)
                            }
                        )
                        Checkbox(
                            checked = false,
                            onCheckedChange = {},
                            enabled = false
                        )

                        if (checked) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = {},
                                enabled = false
                            )
                        }
                    }
                }
            }
        }

        rule.onAllNodes(isToggleable())
            .assertCountEquals(2).apply {
                get(0)
                    .assertIsOff()
                    .performClick()
                    .assertIsOn()
            }

        rule.onAllNodes(isToggleable())
            .assertCountEquals(3).apply {
                get(2)
                    .assertIsOff()
            }
    }

    @Test
    fun findAllTest_twoComponents_toggleDeletesOne() {
        rule.setContent {
            MaterialTheme {
                Surface {
                    val (checked, onCheckedChange) = remember { mutableStateOf(false) }

                    Column {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                onCheckedChange(it)
                            }
                        )
                        if (!checked) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = {},
                                enabled = false
                            )
                        }
                    }
                }
            }
        }

        rule.onAllNodes(isToggleable())
            .assertCountEquals(2)
            .apply {
                get(0)
                    .assertIsOff()
                    .performClick()
                    .assertIsOn()
                get(1)
                    .assertDoesNotExist()
            }
    }
}
