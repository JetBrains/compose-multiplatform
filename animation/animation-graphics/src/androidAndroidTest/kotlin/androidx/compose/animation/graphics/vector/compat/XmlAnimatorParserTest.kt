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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.graphics.vector.AnimatorSet
import androidx.compose.animation.graphics.vector.ObjectAnimator
import androidx.compose.animation.graphics.vector.Ordering
import androidx.compose.animation.graphics.vector.PropertyValuesHolder2D
import androidx.compose.animation.graphics.vector.PropertyValuesHolderColor
import androidx.compose.animation.graphics.vector.PropertyValuesHolderFloat
import androidx.compose.animation.graphics.vector.PropertyValuesHolderInt
import androidx.compose.animation.graphics.vector.PropertyValuesHolderPath
import androidx.compose.animation.graphics.res.AccelerateEasing
import androidx.compose.animation.graphics.res.DecelerateEasing
import androidx.compose.animation.graphics.res.loadAnimatorResource
import androidx.compose.animation.graphics.test.R
import androidx.compose.animation.graphics.vector.RepeatCountInfinite
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class XmlAnimatorParserTest {

    @Test
    fun objectAnimator1D() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val delta = 0.001f
        val a = loadAnimatorResource(
            context.theme,
            resources,
            R.animator.object_animator_1d
        )
        assertThat(a).isInstanceOf(ObjectAnimator::class.java)
        val oa = a as ObjectAnimator
        assertThat(oa.duration).isEqualTo(333)
        assertThat(oa.repeatCount).isEqualTo(1)
        assertThat(oa.repeatMode).isEqualTo(RepeatMode.Reverse)
        assertThat(oa.startDelay).isEqualTo(50)
        assertThat(oa.holders).hasSize(1)
        assertThat(oa.totalDuration).isEqualTo(716)
        val holder = oa.holders[0] as PropertyValuesHolderFloat
        assertThat(holder.propertyName).isEqualTo("translateX")
        assertThat(holder.animatorKeyframes).hasSize(2)
        assertThat(holder.animatorKeyframes[0].value).isWithin(delta).of(0f)
        assertThat(holder.animatorKeyframes[1].value).isWithin(delta).of(100f)
    }

    @Test
    fun objectAnimator2D() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val a = loadAnimatorResource(
            context.theme,
            resources,
            R.animator.object_animator_2d
        )
        assertThat(a).isInstanceOf(ObjectAnimator::class.java)
        val oa = a as ObjectAnimator
        assertThat(oa.duration).isEqualTo(333)
        assertThat(oa.holders).hasSize(1)
        assertThat(oa.totalDuration).isEqualTo(333)
        val holder = oa.holders[0] as PropertyValuesHolder2D
        assertThat(holder.xPropertyName).isEqualTo("translateX")
        assertThat(holder.yPropertyName).isEqualTo("translateY")
        assertThat(holder.pathData).hasSize(3)
        assertThat(holder.interpolator).isEqualTo(LinearEasing)
    }

    @Test
    fun propertyValuesHolders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val delta = 0.001f
        val a = loadAnimatorResource(
            context.theme,
            resources,
            R.animator.property_values_holders
        )
        assertThat(a).isInstanceOf(ObjectAnimator::class.java)
        val oa = a as ObjectAnimator
        assertThat(oa.duration).isEqualTo(333)
        assertThat(oa.repeatCount).isEqualTo(1)
        assertThat(oa.repeatMode).isEqualTo(RepeatMode.Reverse)
        assertThat(oa.startDelay).isEqualTo(50)
        assertThat(oa.holders).hasSize(5)
        assertThat(oa.totalDuration).isEqualTo(716)
        (oa.holders[0] as PropertyValuesHolderFloat).let { holder ->
            assertThat(holder.propertyName).isEqualTo("translateX")
            assertThat(holder.animatorKeyframes).hasSize(2)
            assertThat(holder.animatorKeyframes[0].value).isWithin(delta).of(0f)
            assertThat(holder.animatorKeyframes[1].value).isWithin(delta).of(100f)
        }
        (oa.holders[1] as PropertyValuesHolderFloat).let { holder ->
            assertThat(holder.propertyName).isEqualTo("translateY")
            assertThat(holder.animatorKeyframes).hasSize(4)
            holder.animatorKeyframes[0].let { keyframe ->
                assertThat(keyframe.fraction).isWithin(delta).of(0f)
                assertThat(keyframe.value).isWithin(delta).of(0f)
            }
            holder.animatorKeyframes[1].let { keyframe ->
                assertThat(keyframe.fraction).isWithin(delta).of(0.3f)
                assertThat(keyframe.value).isWithin(delta).of(150f)
                assertThat(keyframe.interpolator).isEqualTo(DecelerateEasing)
            }
            holder.animatorKeyframes[2].let { keyframe ->
                assertThat(keyframe.fraction).isWithin(delta).of(0.6f)
                assertThat(keyframe.value).isWithin(delta).of(50f)
                assertThat(keyframe.interpolator).isEqualTo(AccelerateEasing)
            }
            holder.animatorKeyframes[3].let { keyframe ->
                assertThat(keyframe.fraction).isWithin(delta).of(1f)
                assertThat(keyframe.value).isWithin(delta).of(200f)
            }
        }
        (oa.holders[2] as PropertyValuesHolderColor).let { holder ->
            assertThat(holder.propertyName).isEqualTo("colorProperty")
            assertThat(holder.animatorKeyframes).hasSize(2)
            assertThat(holder.animatorKeyframes[0].value).isEqualTo(Color.Red)
            assertThat(holder.animatorKeyframes[1].value).isEqualTo(Color.Blue)
        }
        (oa.holders[3] as PropertyValuesHolderInt).let { holder ->
            assertThat(holder.propertyName).isEqualTo("intProperty")
            assertThat(holder.animatorKeyframes).hasSize(2)
            assertThat(holder.animatorKeyframes[0].value).isEqualTo(500)
            assertThat(holder.animatorKeyframes[1].value).isEqualTo(1000)
        }
        (oa.holders[4] as PropertyValuesHolderPath).let { holder ->
            assertThat(holder.propertyName).isEqualTo("pathProperty")
            assertThat(holder.animatorKeyframes).hasSize(2)
            assertThat(holder.animatorKeyframes[0].value).hasSize(3)
            assertThat(holder.animatorKeyframes[1].value).hasSize(3)
        }
    }

    @Test
    fun set() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val anim = loadAnimatorResource(
            context.theme,
            resources,
            R.animator.set
        )
        assertThat(anim).isInstanceOf(AnimatorSet::class.java)
        val set = anim as AnimatorSet
        assertThat(set.ordering).isEqualTo(Ordering.Together)
        assertThat(set.animators).hasSize(2)
        assertThat(set.totalDuration).isEqualTo(300)
        (set.animators[0] as ObjectAnimator).let { oa ->
            assertThat(oa.duration).isEqualTo(300)
            assertThat(oa.repeatCount).isEqualTo(0)
            assertThat(oa.startDelay).isEqualTo(0)
            assertThat(oa.holders).hasSize(1)
            (oa.holders[0] as PropertyValuesHolderFloat).let { holder ->
                assertThat(holder.propertyName).isEqualTo("floatProperty")
            }
        }
        (set.animators[1] as AnimatorSet).let { s ->
            assertThat(s.ordering).isEqualTo(Ordering.Sequentially)
            assertThat(s.animators).hasSize(2)
            (s.animators[0] as ObjectAnimator).let { oa ->
                assertThat(oa.holders).hasSize(1)
                assertThat(oa.repeatCount).isEqualTo(RepeatCountInfinite)
                (oa.holders[0] as PropertyValuesHolderInt).let { holder ->
                    assertThat(holder.propertyName).isEqualTo("intProperty")
                }
            }
            (s.animators[1] as ObjectAnimator).let { oa ->
                assertThat(oa.holders).hasSize(1)
                (oa.holders[0] as PropertyValuesHolderColor).let { holder ->
                    assertThat(holder.propertyName).isEqualTo("colorProperty")
                }
            }
        }
    }
}
