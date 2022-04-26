/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.layout.PinnableParent.PinnedItemsHandle
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PinnableParentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noPinnableParent_returnsNull() {
        // Arrange.
        var pinnableParent: PinnableParent? = null
        rule.setContent {
            Box(Modifier.onPinnableParentAvailable { pinnableParent = it })
        }

        // Assert.
        assertThat(pinnableParent).isNull()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun returnsPinnableParent() {
        // Arrange.
        val providedPinnableParent = TestPinnableParent()
        var receivedPinnableParent: PinnableParent? = null
        rule.setContent {
            Box(
                Modifier
                    .modifierLocalProvider(ModifierLocalPinnableParent) { providedPinnableParent }
                    .onPinnableParentAvailable { receivedPinnableParent = it }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(receivedPinnableParent).isEqualTo(providedPinnableParent)
        }
    }

    private class TestPinnableParent : PinnableParent {
        override fun pinItems(): PinnedItemsHandle {
            TODO("Not yet implemented")
        }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    private fun Modifier.onPinnableParentAvailable(
        onPinnableParentAvailable: (PinnableParent?) -> Unit
    ): Modifier = modifierLocalConsumer {
        onPinnableParentAvailable.invoke(ModifierLocalPinnableParent.current)
    }
}
