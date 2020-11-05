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

package androidx.compose.ui.test.assertions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.testutils.expectError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class BoundsAssertionsTest {

    @get:Rule
    val rule = createComposeRule()

    val tag = "box"

    private fun composeBox() {
        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
            ) {
                Box(modifier = Modifier.padding(start = 50.dp, top = 100.dp)) {
                    Box(
                        modifier = Modifier
                            .testTag(tag)
                            .size(80.dp, 100.dp)
                            .background(color = Color.Black)
                    )
                }
            }
        }
    }

    @Test
    fun dp_assertEquals() {
        5.dp.assertIsEqualTo(5.dp)
        5.dp.assertIsEqualTo(4.6.dp)
        5.dp.assertIsEqualTo(5.4.dp)
    }

    @Test
    fun dp_assertNotEquals() {
        5.dp.assertIsNotEqualTo(6.dp)
    }

    @Test
    fun dp_assertEquals_fail() {
        expectError<AssertionError> {
            5.dp.assertIsEqualTo(6.dp)
        }
    }

    @Test
    fun dp_assertNotEquals_fail() {
        expectError<AssertionError> {
            5.dp.assertIsNotEqualTo(5.dp)
            5.dp.assertIsNotEqualTo(5.4.dp)
        }
    }

    @Test
    fun assertSizeEquals() {
        composeBox()

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun assertSizeAtLeast() {
        composeBox()

        rule.onNodeWithTag(tag)
            .assertWidthIsAtLeast(80.dp)
            .assertWidthIsAtLeast(79.dp)
            .assertHeightIsAtLeast(100.dp)
            .assertHeightIsAtLeast(99.dp)
    }

    @Test
    fun assertSizeEquals_fail() {
        composeBox()

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertWidthIsEqualTo(70.dp)
        }

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertHeightIsEqualTo(90.dp)
        }
    }

    @Test
    fun assertSizeAtLeast_fail() {
        composeBox()

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertWidthIsAtLeast(81.dp)
        }

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertHeightIsAtLeast(101.dp)
        }
    }

    @Test
    fun assertPosition() {
        composeBox()

        rule.onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = 50.dp, expectedTop = 100.dp)
            .assertLeftPositionInRootIsEqualTo(50.dp)
            .assertTopPositionInRootIsEqualTo(100.dp)
    }

    @Test
    fun assertPosition_fail() {
        composeBox()

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 51.dp, expectedTop = 101.dp)
        }

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 49.dp, expectedTop = 99.dp)
        }
    }

    private fun composeClippedBox() {
        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .wrapContentSize(Alignment.TopStart)
            ) {
                // Box is shifted 30dp to the left and 10dp to the top,
                // so it is clipped to a size of 50 x 90
                Box(
                    modifier = Modifier
                        .testTag(tag)
                        .offset((-30).dp, (-10).dp)
                        .size(80.dp, 100.dp)
                        .background(color = Color.Black)
                )
            }
        }
    }

    @Test
    fun assertClippedPosition() {
        composeClippedBox()

        rule.onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = (-30).dp, expectedTop = (-10).dp)
            .assertLeftPositionInRootIsEqualTo((-30).dp)
            .assertTopPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun assertClippedSize() {
        composeClippedBox()

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(100.dp)
    }
}
