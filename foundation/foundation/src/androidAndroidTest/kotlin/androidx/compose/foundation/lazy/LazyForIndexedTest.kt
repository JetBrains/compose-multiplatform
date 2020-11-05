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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class LazyForIndexedTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun columnWithIndexesComposedWithCorrectIndexAndItem() {
        val items = (0..1).map { it.toString() }

        rule.setContent {
            LazyColumnForIndexed(items, Modifier.preferredHeight(200.dp)) { index, item ->
                BasicText("${index}x$item", Modifier.fillParentMaxWidth().height(100.dp))
            }
        }

        rule.onNodeWithText("0x0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithText("1x1")
            .assertTopPositionInRootIsEqualTo(100.dp)
    }

    @Test
    fun rowWithIndexesComposedWithCorrectIndexAndItem() {
        val items = (0..1).map { it.toString() }

        rule.setContent {
            LazyRowForIndexed(items, Modifier.preferredWidth(200.dp)) { index, item ->
                BasicText("${index}x$item", Modifier.fillParentMaxHeight().width(100.dp))
            }
        }

        rule.onNodeWithText("0x0")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithText("1x1")
            .assertLeftPositionInRootIsEqualTo(100.dp)
    }
}
