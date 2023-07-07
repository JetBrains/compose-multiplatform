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

package androidx.compose.ui.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class ClickCounterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Counter()
        }
    }
}

class EmptyActivity : ComponentActivity()

@Composable
fun Counter() {
    var counter by remember { mutableStateOf(0) }
    Column {
        Button(onClick = { counter++ }) {
            Text("Increment counter")
        }
        Text(text = "Clicks: $counter")
    }
}

@RunWith(Parameterized::class)
class ClickTestRuleTest(private val config: TestConfig) {
    data class TestConfig(
        val activityClass: Class<out ComponentActivity>?,
        val setContentInTest: Boolean
    ) {
        override fun toString(): String =
            "activity=${activityClass?.simpleName}, rule.setContent=$setContentInTest"
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters() =
            listOf(
                TestConfig(ClickCounterActivity::class.java, false),
                TestConfig(EmptyActivity::class.java, true),
                TestConfig(null, true),
            )
    }

    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = when (config.activityClass) {
        null -> createComposeRule()
        else -> createAndroidComposeRule(config.activityClass)
    }

    @Test
    fun testClick() {
        if (config.setContentInTest) {
            composeTestRule.setContent {
                Counter()
            }
        }
        composeTestRule.onNodeWithText("Increment counter").performClick()
        composeTestRule.onNodeWithText("Clicks: 1").assertExists()
    }
}
