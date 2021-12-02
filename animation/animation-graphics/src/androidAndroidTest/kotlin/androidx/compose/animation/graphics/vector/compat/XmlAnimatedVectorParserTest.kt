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

package androidx.compose.animation.graphics.vector.compat

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.loadAnimatedVectorResource
import androidx.compose.animation.graphics.test.R
import androidx.compose.animation.graphics.vector.AnimatorSet
import androidx.compose.animation.graphics.vector.ObjectAnimator
import androidx.compose.animation.graphics.vector.PropertyValuesHolderColor
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationGraphicsApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class XmlAnimatedVectorParserTest {

    @Test
    fun load() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val avd = loadAnimatedVectorResource(
            context.theme,
            resources,
            R.drawable.avd_complex
        )

        val delta = 0.001f
        assertThat(avd.imageVector.defaultWidth).isEqualTo(24.dp)
        assertThat(avd.imageVector.defaultHeight).isEqualTo(24.dp)
        assertThat(avd.imageVector.viewportWidth).isWithin(delta).of(24f)
        assertThat(avd.imageVector.viewportHeight).isWithin(delta).of(24f)

        assertThat(avd.targets).hasSize(1)

        avd.targets[0].let { target ->
            assertThat(target.name).isEqualTo("background")
            assertThat(target.animator).isInstanceOf(AnimatorSet::class.java)
            (target.animator as AnimatorSet).let { set ->
                assertThat(set.animators).hasSize(1)
                (set.animators[0] as ObjectAnimator).let { a ->
                    assertThat(a.duration).isEqualTo(123)
                    assertThat(a.holders).hasSize(1)
                    (a.holders[0] as PropertyValuesHolderColor).let { holder ->
                        assertThat(holder.propertyName).isEqualTo("fillColor")
                    }
                }
            }
        }
    }
}
