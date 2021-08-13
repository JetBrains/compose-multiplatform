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

package androidx.compose.foundation.gesture

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.timeNowMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RunWith(AndroidJUnit4::class)
class StretchOverscrollTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @Ignore("platform animaiton are turned off. Figure this out in b/197325932")
    fun stretchOverscroll_whenPulled_consumesOppositePreScroll() {
        val color = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green)
        val lazyState = LazyListState()
        rule.setContent {
            LazyRow(
                state = lazyState,
                modifier = Modifier.size(300.dp).testTag(OverscrollBox)
            ) {
                items(10) { index ->
                    Box(Modifier.size(50.dp, 300.dp).background(color[index % color.size]))
                }
            }
        }

        var now = timeNowMillis()
        rule.waitUntil(10000) { timeNowMillis() - now > 3000 }

        rule.onNodeWithTag(OverscrollBox).performGesture {
            down(center)
            moveBy(Offset(200f, 0f))
            moveBy(Offset(200f, 0f))
            moveBy(Offset(200f, 0f))
        }

        now = timeNowMillis()
        rule.waitUntil(10000) { timeNowMillis() - now > 3000 }

        rule.onNodeWithTag(OverscrollBox).performGesture {
            // pull in the opposite direction. Since we pulled overscroll with positive delta
            // it will consume negative delta before scroll happens
            // assert in the ScrollableState lambda will check it
            moveBy(Offset(-30f, 0f))
            up()
        }
        rule.runOnIdle {
            // no scroll happened as it was consumed by the overscroll logic
            assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        }
    }
}

private const val OverscrollBox = "box"