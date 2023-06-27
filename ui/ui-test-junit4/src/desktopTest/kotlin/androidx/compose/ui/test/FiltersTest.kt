/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test


/**
 * Tests the filters (e.g. [hasParent]) functionality of the testing framework.
 */
class FiltersTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Ignore  // TODO: Fix Dialog to have the dialog() semantic property
    fun testIsDialog() {
        rule.setContent {
            Dialog(
                onCloseRequest = {},
            ){
                Text(
                    text = "Text",
                    modifier = Modifier.testTag("tag")
                )

            }
        }

        rule.onNodeWithTag("tag").assert(hasAnyAncestor(isDialog()))
    }

    @Test
    @Ignore  // TODO: Fix Popup to have the popup() semantic property
    fun testIsPopup() {
        rule.setContent {
            Popup{
                Text(
                    text = "Text",
                    modifier = Modifier.testTag("tag")
                )

            }
        }

        rule.onNodeWithTag("tag").assert(hasAnyAncestor(isPopup()))
    }

    @Test
    fun testHasAnyChild() {
        rule.setContent {
            Box(Modifier.testTag("box")){
                Text(text = "text")
            }
        }

        rule.onNodeWithTag("box").assert(hasAnyChild(hasText("text")))
    }

    @Test
    fun testHasAnyAncestor() {
        rule.setContent {
            Box(Modifier.testTag("ancestor1")){
                Box(Modifier.testTag("ancestor2")){
                    Text(text = "text")
                }
            }
        }

        rule.onNodeWithText("text").assert(hasAnyAncestor(hasTestTag("ancestor1")))
        rule.onNodeWithText("text").assert(hasAnyAncestor(hasTestTag("ancestor2")))
    }

    @Test
    fun testHasAnyParent() {
        rule.setContent {
            Box(Modifier.testTag("parent")){
                Text(text = "text")
            }
        }

        rule.onNodeWithText("text").assert(hasParent(hasTestTag("parent")))
    }

    @Test
    fun testHasAnySibling() {
        rule.setContent {
            Box{
                Text(text = "text1")
                Text(text = "text2")
            }
        }

        rule.onNodeWithText("text1").assert(hasAnySibling(hasText("text2")))
    }

    @Test
    fun testIsRoot() {
        rule.setContent {
            Text(text = "text")
        }

        rule.onNodeWithText("text").assert(hasParent(isRoot()))
    }

    @Test
    fun testHasSetTextAction() {
        rule.setContent {
            TextField(
                value = "text",
                onValueChange = {}
            )
        }

        rule.onNodeWithText("text").assert(hasSetTextAction())
    }

    @Test
    fun testHasScrollAction() {
        rule.setContent {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("tag")
            ) {
                Text("1")
                Text("2")
            }
        }

        rule.onNodeWithTag("tag").assert(hasScrollAction())
    }

    @Test
    fun testHasNoScrollAction() {
        rule.setContent {
            Column(
                Modifier.testTag("tag")
            ) {
                Text("1")
                Text("2")
            }
        }

        rule.onNodeWithTag("tag").assert(hasNoScrollAction())
    }

    @Test
    fun testHasScrollToIndexAction() {
        rule.setContent {
            LazyColumn(
                Modifier.testTag("tag")
            ) {
                items(2){
                    Text("$it")
                }
            }
        }

        rule.onNodeWithTag("tag").assert(hasScrollToIndexAction())
    }

    @Test
    fun testHasScrollToKeyAction() {
        rule.setContent {
            LazyColumn(
                Modifier.testTag("tag")
            ) {
                items(2){
                    Text("$it")
                }
            }
        }

        rule.onNodeWithTag("tag").assert(hasScrollToKeyAction())
    }

    @Test
    fun testHasScrollToNodeAction() {
        rule.setContent {
            LazyColumn(
                Modifier.testTag("tag")
            ) {
                items(2){
                    Text("$it")
                }
            }
        }

        rule.onNodeWithTag("tag").assert(hasScrollToNodeAction())
    }
}