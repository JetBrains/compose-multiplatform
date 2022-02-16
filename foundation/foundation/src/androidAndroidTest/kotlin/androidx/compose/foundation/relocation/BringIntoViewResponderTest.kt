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

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TestActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class BringIntoViewResponderTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    fun Float.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    @Test
    fun zeroSizedItem_zeroSizedParent_bringIntoView() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var requestedRect: Rect
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable { requestedRect = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRect).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun bringIntoView_rectInChild() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var requestedRect: Rect
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable { requestedRect = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView(Rect(1f, 2f, 3f, 4f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRect).isEqualTo(Rect(1f, 2f, 3f, 4f))
        }
    }

    @Test
    fun bringIntoView_childWithSize() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var requestedRect: Rect
        rule.setContent {
            Box(Modifier) {
                Box(
                    Modifier
                        .fakeScrollable { requestedRect = it }
                        .size(20f.toDp(), 10f.toDp())
                        .offset { IntOffset(40, 30) }
                        .bringIntoViewRequester(bringIntoViewRequester)
                )
            }
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRect).isEqualTo(Rect(40f, 30f, 60f, 40f))
        }
    }

    @Test
    fun bringIntoView_childBiggerThanParent() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var requestedRect: Rect
        rule.setContent {
            Box(
                Modifier
                    .size(1f.toDp())
                    .fakeScrollable { requestedRect = it }
                    .size(20f.toDp(), 10f.toDp())
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRect).isEqualTo(Rect(0f, 0f, 20f, 10f))
        }
    }

    @Test
    fun bringIntoView_propagatesToMultipleResponders() {
        // Arrange.
        lateinit var outerRequest: Rect
        lateinit var innerRequest: Rect
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable { outerRequest = it }
                    .offset(2f.toDp(), 1f.toDp())
                    .fakeScrollable { innerRequest = it }
                    .size(20f.toDp(), 10f.toDp())
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(innerRequest).isEqualTo(Rect(0f, 0f, 20f, 10f))
            assertThat(outerRequest).isEqualTo(Rect(2f, 1f, 22f, 11f))
        }
    }

    @Test
    fun bringIntoView_onlyPropagatesUp() {
        // Arrange.
        lateinit var parentRequest: Rect
        var childRequest: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable { parentRequest = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .fakeScrollable { childRequest = it }
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentRequest).isEqualTo(Rect.Zero)
            assertThat(childRequest).isNull()
        }
    }

    @Test
    fun bringIntoView_propagatesUp_whenRectForParentReturnsInput() {
        // Arrange.
        lateinit var parentRequest: Rect
        var childRequest: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable { parentRequest = it }
                    .fakeScrollable { childRequest = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentRequest).isEqualTo(Rect.Zero)
            assertThat(childRequest).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun bringIntoView_translatesByCalculateRectForParent() {
        // Arrange.
        lateinit var requestedRect: Rect
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable { requestedRect = it }
                    .fakeScrollable(Offset(2f, 3f)) {}
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRect).isEqualTo(Rect(2f, 3f, 2f, 3f))
        }
    }

    @Test
    fun bringChildIntoView_isCalled_whenRectForParentDoesNotReturnInput() {
        // Arrange.
        var requestedRect: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable(Offset.Zero) { requestedRect = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRect).isEqualTo(Rect.Zero)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun bringChildIntoView_calledConcurrentlyOnAllResponders() {
        // Arrange.
        var childStarted = false
        var parentStarted = false
        var childFinished = false
        var parentFinished = false
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier
                    .fakeScrollable {
                        parentStarted = true
                        try {
                            awaitCancellation()
                        } finally {
                            parentFinished = true
                        }
                    }
                    .fakeScrollable {
                        childStarted = true
                        try {
                            awaitCancellation()
                        } finally {
                            childFinished = true
                        }
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }
        val testScope = TestCoroutineScope().apply {
            pauseDispatcher()
        }
        val requestJob = testScope.launch {
            bringIntoViewRequester.bringIntoView()
        }
        rule.waitForIdle()

        assertThat(childStarted).isFalse()
        assertThat(parentStarted).isFalse()
        assertThat(childFinished).isFalse()
        assertThat(parentFinished).isFalse()

        // Act.
        testScope.advanceUntilIdle()

        // Assert.
        assertThat(childStarted).isTrue()
        assertThat(parentStarted).isTrue()
        assertThat(childFinished).isFalse()
        assertThat(parentFinished).isFalse()

        // Act.
        requestJob.cancel()
        testScope.advanceUntilIdle()

        // Assert.
        assertThat(childFinished).isTrue()
        assertThat(parentFinished).isTrue()
    }
}