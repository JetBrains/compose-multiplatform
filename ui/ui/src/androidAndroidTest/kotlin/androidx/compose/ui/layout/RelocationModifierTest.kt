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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinateSubject.Companion.assertThat
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class RelocationModifierTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    lateinit var density: Density

    @Before
    fun setup() {
        density = Density(rule.activity)
    }

    fun Float.toDp(): Dp = with(density) { this@toDp.toDp() }

    @Test
    fun zeroSizedItem_zeroSizedParent_bringIntoView_noOffsetNoRelocation() {
        // Arrange.
        lateinit var childRect: Rect
        lateinit var source: Rect
        lateinit var destination: Rect
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .onRelocationRequest(
                        onProvideDestination = { rect, _ ->
                            childRect = rect
                            Rect.Zero
                        },
                        onPerformRelocation = { sourceRect, destinationRect ->
                            source = sourceRect
                            destination = destinationRect
                        }
                    )
                    .relocationRequester(relocationRequester)
            )
        }

        // Act.
        runBlocking { relocationRequester.bringIntoView() }

        // Assert.
        assertThat(childRect).isEqualTo(Rect.Zero)
        assertThat(source).isEqualTo(Rect.Zero)
        assertThat(destination).isEqualTo(Rect.Zero)
    }

    @Test
    fun bringIntoView_onPerformRelocationCalledWithSameSourceAndDestination() {
        // Arrange.
        lateinit var childRect: Rect
        lateinit var source: Rect
        lateinit var destination: Rect
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .size(20f.toDp(), 10f.toDp())
                    .onRelocationRequest(
                        onProvideDestination = { rect, _ ->
                            childRect = rect
                            rect
                        },
                        onPerformRelocation = { sourceRect, destinationRect ->
                            source = sourceRect
                            destination = destinationRect
                        }
                    )
                    .relocationRequester(relocationRequester)
            )
        }

        // Act.
        runBlocking { relocationRequester.bringIntoView() }

        // Assert.
        assertThat(childRect).isEqualTo(Rect(0f, 0f, 20f, 10f))
        assertThat(source).isEqualTo(childRect)
        assertThat(destination).isEqualTo(source)
    }

    @Test
    fun bringIntoView_onPerformRelocationCalledWithSameSourceAsOnProvideDestination() {
        // Arrange.
        lateinit var childRect: Rect
        lateinit var source: Rect
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .size(20.dp, 10.dp)
                    .onRelocationRequest(
                        onProvideDestination = { rect, _ ->
                            childRect = rect
                            Rect(7f, 7f, 7f, 7f)
                        },
                        onPerformRelocation = { sourceRect, _ -> source = sourceRect }
                    )
                    .relocationRequester(relocationRequester)
            )
        }

        // Act.
        runBlocking { relocationRequester.bringIntoView() }

        // Assert.
        assertThat(source).isEqualTo(childRect)
    }

    @Test
    fun bringIntoView_withOffset_onPerformRelocationCalledWithSpecifiedDestination() {
        // Arrange.
        lateinit var destination: Rect
        val relocationRequester = RelocationRequester()
        val specifiedDestination = Rect(5f, 5f, 5f, 5f)
        rule.setContent {
            Box(
                Modifier
                    .size(20.dp, 10.dp)
                    .onRelocationRequest(
                        onProvideDestination = { _, _ -> specifiedDestination },
                        onPerformRelocation = { _, destinationRect ->
                            destination = destinationRect
                        }
                    )
                    .relocationRequester(relocationRequester)
            )
        }

        // Act.
        runBlocking { relocationRequester.bringIntoView() }

        // Assert.
        assertThat(destination).isEqualTo(specifiedDestination)
    }

    @Test
    fun bringIntoView_onProvideDestination_receivesLayoutCoordinates() {
        // Arrange.
        lateinit var onProvideDestinationCoordinates: LayoutCoordinates
        lateinit var onGloballyPositionedCoordinates: LayoutCoordinates
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .size(20.dp, 10.dp)
                    .onRelocationRequest(
                        onProvideDestination = { rect, coordinates ->
                            onProvideDestinationCoordinates = coordinates
                            rect
                        },
                        onPerformRelocation = { _, _ -> }
                    )
                    .relocationRequester(relocationRequester)
                    .onGloballyPositioned { onGloballyPositionedCoordinates = it }
            )
        }

        // Act.
        runBlocking { relocationRequester.bringIntoView() }

        // Assert.
        assertThat(onProvideDestinationCoordinates).isEqualTo(onGloballyPositionedCoordinates)
    }

    @Test
    fun onRelocationRequest_parentIsCalledWithDestinationSpecifiedByChild() {
        // Arrange.
        lateinit var childRect2: Rect
        val relocationRequester = RelocationRequester()
        val specifiedDestination2 = Rect(10f, 10f, 10f, 10f)
        val specifiedDestination1 = Rect(5f, 5f, 5f, 5f)
        rule.setContent {
            Box(
                Modifier
                    .size(40.dp, 30.dp)
                    .onRelocationRequest(
                        onProvideDestination = { rect, _ ->
                            childRect2 = rect
                            specifiedDestination2
                        },
                        onPerformRelocation = { _, _ -> }
                    )
            ) {
                Box(
                    Modifier
                        .size(20.dp, 10.dp)
                        .onRelocationRequest(
                            onProvideDestination = { _, _ -> specifiedDestination1 },
                            onPerformRelocation = { _, _ -> }
                        )
                        .relocationRequester(relocationRequester)
                )
            }
        }

        // Act.
        runBlocking { relocationRequester.bringIntoView() }

        // Assert.
        assertThat(childRect2).isEqualTo(specifiedDestination1)
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
