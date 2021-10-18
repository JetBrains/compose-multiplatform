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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewResponder.Companion.ModifierLocalBringIntoViewResponder
import androidx.compose.foundation.relocation.LayoutCoordinateSubject.Companion.assertThat
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.foundation.TestActivity
import androidx.compose.foundation.relocation.ScrollableResponder.Companion.LocalRect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BringIntoViewResponderTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    lateinit var density: Density

    @Before
    fun setup() {
        density = Density(rule.activity)
    }

    fun Float.toDp(): Dp = with(density) { this@toDp.toDp() }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun zeroSizedItem_zeroSizedParent_bringIntoView() {
        // Arrange.
        lateinit var layoutCoordinates: LayoutCoordinates
        val bringIntoViewRequester = BringIntoViewRequester()
        val scrollableParent = ScrollableResponder()
        rule.setContent {
            Box(
                Modifier
                    .modifierLocalProvider(ModifierLocalBringIntoViewResponder) { scrollableParent }
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onGloballyPositioned { layoutCoordinates = it }
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(scrollableParent.callersLayoutCoordinates).isEqualTo(layoutCoordinates)
        assertThat(scrollableParent.nonLocalRect).isEqualTo(Rect.Zero)
        assertThat(scrollableParent.requestedRect).isEqualTo(LocalRect)
    }

    @OptIn(ExperimentalComposeUiApi::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
    )
    @Test
    fun bringIntoView_itemWithSize() {
        // Arrange.
        lateinit var layoutCoordinates: LayoutCoordinates
        val bringIntoViewRequester = BringIntoViewRequester()
        val scrollableParent = ScrollableResponder()
        rule.setContent {
            Box(
                Modifier
                    .size(20f.toDp(), 10f.toDp())
                    .modifierLocalProvider(ModifierLocalBringIntoViewResponder) { scrollableParent }
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onGloballyPositioned { layoutCoordinates = it }
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        // Assert.
        assertThat(scrollableParent.callersLayoutCoordinates).isEqualTo(layoutCoordinates)
        assertThat(scrollableParent.nonLocalRect).isEqualTo(Rect(0f, 0f, 20f, 10f))
        assertThat(scrollableParent.requestedRect).isEqualTo(LocalRect)
    }
}

private class LayoutCoordinateSubject(metadata: FailureMetadata?, val actual: LayoutCoordinates?) :
    Subject(metadata, actual) {

    fun isEqualTo(expected: LayoutCoordinates?) {
        if (expected == null) {
            assertThat(actual).isNull()
            return
        }
        if (actual == null) {
            failWithActual(simpleFact("Expected non-null layout coordinates."))
            return
        }
        if (expected.size != actual.size) {
            failWithoutActual(
                simpleFact("Expected size be ${expected.size}"),
                simpleFact("But was ${actual.size}")
            )
        }
        if (expected.positionInParent() != actual.positionInParent()) {
            failWithoutActual(
                simpleFact("Expected bounds in parent to be ${expected.boundsInParent()}"),
                simpleFact("But was ${actual.boundsInParent()}")
            )
        }
    }

    companion object {
        fun assertThat(layoutCoordinates: LayoutCoordinates?): LayoutCoordinateSubject {
            return Truth.assertAbout(subjectFactory).that(layoutCoordinates)
        }

        private val subjectFactory =
            Factory<LayoutCoordinateSubject, LayoutCoordinates?> { metadata, actual ->
                LayoutCoordinateSubject(metadata, actual)
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class ScrollableResponder : BringIntoViewResponder {
    lateinit var requestedRect: Rect
    lateinit var nonLocalRect: Rect
    lateinit var callersLayoutCoordinates: LayoutCoordinates

    override suspend fun bringIntoView(rect: Rect) {
        requestedRect = rect
    }

    override fun toLocalRect(rect: Rect, layoutCoordinates: LayoutCoordinates): Rect {
        nonLocalRect = rect
        callersLayoutCoordinates = layoutCoordinates
        return LocalRect
    }

    companion object {
        val LocalRect = Rect(10f, 10f, 10f, 10f)
    }
}
