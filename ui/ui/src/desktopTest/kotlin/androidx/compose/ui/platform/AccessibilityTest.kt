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

import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.accessibility.AccessibleText

@RunWith(JUnit4::class)
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun accessibleText() {
        rule.setContent {
            Text("Hello world. Hi world.", modifier = Modifier.testTag("text"))
        }

        val node = rule.onNodeWithTag("text").fetchSemanticsNode()
        val accessibleNode = ComposeAccessible(node)
        val accessibleText = accessibleNode.accessibleContext.accessibleText!!
        assertEquals(22, accessibleText.charCount)

        assertEquals("H", accessibleText.getAtIndex(AccessibleText.CHARACTER, 0))
        assertEquals("Hello", accessibleText.getAtIndex(AccessibleText.WORD, 0))
        assertEquals("Hello world. ", accessibleText.getAtIndex(AccessibleText.SENTENCE, 0))

        assertEquals("e", accessibleText.getAfterIndex(AccessibleText.CHARACTER, 0))
        assertEquals("world", accessibleText.getAfterIndex(AccessibleText.WORD, 0))
        assertEquals("Hi world.", accessibleText.getAfterIndex(AccessibleText.SENTENCE, 0))

        assertEquals("d", accessibleText.getBeforeIndex(AccessibleText.CHARACTER, 21))
        assertEquals("world", accessibleText.getBeforeIndex(AccessibleText.WORD, 21))
        assertEquals("Hi world", accessibleText.getBeforeIndex(AccessibleText.SENTENCE, 21))
    }
}