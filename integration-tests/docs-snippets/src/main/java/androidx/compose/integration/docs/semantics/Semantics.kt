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

@file:Suppress("unused", "UNUSED_PARAMETER")

package androidx.compose.integration.docs.semantics

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/semantics
 *
 * No action required if it's modified.
 */

private object SemanticsSnippet1 {
    class MyComposeTest {

        @get:Rule
        val composeTestRule = createComposeRule()

        @Test
        fun MyTest() {
            // Start the app
            composeTestRule.setContent {
                MyTheme {
                    Text("Hello world!")
                }
            }
            // Log the full semantics tree
            composeTestRule.onRoot().printToLog("MY TAG")
        }
    }
}

private fun SemanticsSnippet2() {
    val mySwitch = SemanticsMatcher.expectValue(
        SemanticsProperties.Role, Role.Switch
    )
    composeTestRule.onNode(mySwitch)
        .performClick()
        .assertIsOff()
}

@Composable private fun SemanticsSnippet3() {
    Button(onClick = { /*TODO*/ }) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Like")
    }
}

private fun SemanticsSnippet4() {
    composeTestRule.onRoot(useUnmergedTree = true).printToLog("MY TAG")
}

private fun SemanticsSnippet5() {
    composeTestRule.onNodeWithText("Like").performClick()
}

/*
Fakes needed for snippets to build:
 */

@Composable private fun MyTheme(content: @Composable () -> Unit) {}
private val composeTestRule = createComposeRule()