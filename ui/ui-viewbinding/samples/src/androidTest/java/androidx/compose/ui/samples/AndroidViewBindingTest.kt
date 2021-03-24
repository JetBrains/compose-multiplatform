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

package androidx.compose.ui.samples

import android.os.Build
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.viewbinding.samples.databinding.SampleLayoutBinding
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AndroidViewBindingTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun drawing() {
        rule.setContent {
            AndroidViewBinding(SampleLayoutBinding::inflate, Modifier.testTag("layout"))
        }

        val size = 50.dp
        val sizePx = with(rule.density) { size.roundToPx() }
        rule.onNodeWithTag("layout").captureToImage().assertPixels(IntSize(sizePx, sizePx * 2)) {
            if (it.y < sizePx) Color.Blue else Color.Black
        }
    }

    @Test
    @LargeTest
    fun update() {
        val color = mutableStateOf(Color.Gray)
        rule.setContent {
            AndroidViewBinding(SampleLayoutBinding::inflate, Modifier.testTag("layout")) {
                second.setBackgroundColor(color.value.toArgb())
            }
        }

        val size = 50.dp
        val sizePx = with(rule.density) { size.roundToPx() }
        rule.onNodeWithTag("layout").captureToImage()
            .assertPixels(IntSize(sizePx, sizePx * 2)) {
                if (it.y < sizePx) Color.Blue else color.value
            }

        rule.runOnIdle { color.value = Color.DarkGray }
        rule.onNodeWithTag("layout").captureToImage()
            .assertPixels(IntSize(sizePx, sizePx * 2)) {
                if (it.y < sizePx) Color.Blue else color.value
            }
    }

    @Test
    fun propagatesDensity() {
        rule.setContent {
            val size = 50.dp
            val density = Density(3f)
            val sizeIpx = with(density) { size.roundToPx() }
            CompositionLocalProvider(LocalDensity provides density) {
                AndroidViewBinding(
                    SampleLayoutBinding::inflate,
                    Modifier.requiredSize(size).onGloballyPositioned {
                        Truth.assertThat(it.size).isEqualTo(IntSize(sizeIpx, sizeIpx))
                    }
                )
            }
        }
        rule.waitForIdle()
    }
}
