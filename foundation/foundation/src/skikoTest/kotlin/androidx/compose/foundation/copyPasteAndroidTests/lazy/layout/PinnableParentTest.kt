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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.layout.ModifierLocalPinnableParent
import androidx.compose.foundation.lazy.layout.PinnableParent
import androidx.compose.foundation.lazy.layout.PinnableParent.PinnedItemsHandle
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
class PinnableParentTest {

    @Test
    fun noPinnableParent_returnsNull() = runSkikoComposeUiTest {
        // Arrange.
        var pinnableParent: PinnableParent? = null
        setContent {
            Box(Modifier.onPinnableParentAvailable { pinnableParent = it })
        }

        // Assert.
        assertThat(pinnableParent).isNull()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun returnsPinnableParent() = runSkikoComposeUiTest {
        // Arrange.
        val providedPinnableParent = TestPinnableParent()
        var receivedPinnableParent: PinnableParent? = null
        setContent {
            Box(
                Modifier
                    .modifierLocalProvider(ModifierLocalPinnableParent) { providedPinnableParent }
                    .onPinnableParentAvailable { receivedPinnableParent = it }
            )
        }

        // Assert.
        runOnIdle {
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
