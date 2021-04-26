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

package androidx.compose.material.ripple

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [RippleContainer]
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RippleContainerTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cachesViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance = createRippleIndicationInstance(container)

        with(container) {
            val hostView = instance.getRippleHostView()
            // The same View should be returned
            Truth.assertThat(hostView).isEqualTo(instance.getRippleHostView())
        }
    }

    @Test
    fun returnsNewViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance1 = createRippleIndicationInstance(container)
        val instance2 = createRippleIndicationInstance(container)

        with(container) {
            val hostView1 = instance1.getRippleHostView()
            val hostView2 = instance2.getRippleHostView()
            // A new View should be returned
            Truth.assertThat(hostView1).isNotEqualTo(hostView2)
        }
    }

    @Test
    fun reassignsExistingViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance1 = createRippleIndicationInstance(container)
        val instance2 = createRippleIndicationInstance(container)
        val instance3 = createRippleIndicationInstance(container)
        val instance4 = createRippleIndicationInstance(container)
        val instance5 = createRippleIndicationInstance(container)
        val instance6 = createRippleIndicationInstance(container)

        with(container) {
            // Assign the maximum number of host views
            val hostView1 = instance1.getRippleHostView()
            val hostView2 = instance2.getRippleHostView()
            instance3.getRippleHostView()
            instance4.getRippleHostView()
            instance5.getRippleHostView()

            // When we try and get a new view on the 6th instance
            val hostView6 = instance6.getRippleHostView()

            // It should be the same as hostView1, now re-assigned to a new view
            Truth.assertThat(hostView6).isEqualTo(hostView1)

            // When the first instance tries to get the instance again
            val hostView = instance1.getRippleHostView()

            // It should now be the same view used for the second instance, as we continue to
            // recycle in order
            Truth.assertThat(hostView).isNotEqualTo(hostView6)
            Truth.assertThat(hostView).isEqualTo(hostView2)
        }
    }

    @Test
    fun reusesDisposedViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance1 = createRippleIndicationInstance(container)
        val instance2 = createRippleIndicationInstance(container)
        val instance3 = createRippleIndicationInstance(container)
        val instance4 = createRippleIndicationInstance(container)
        val instance5 = createRippleIndicationInstance(container)
        val instance6 = createRippleIndicationInstance(container)

        with(container) {
            // Assign some initial views
            val hostView1 = instance1.getRippleHostView()
            val hostView2 = instance2.getRippleHostView()
            val hostView3 = instance3.getRippleHostView()

            // Dispose the first two ripples
            instance1.disposeRippleIfNeeded()
            instance2.disposeRippleIfNeeded()

            // The host views previously used by instance1 and instance1 should now be reused,
            // before allocating new views
            val hostView4 = instance4.getRippleHostView()
            val hostView5 = instance5.getRippleHostView()

            Truth.assertThat(hostView4).isEqualTo(hostView1)
            Truth.assertThat(hostView5).isEqualTo(hostView2)

            // When we try and get a view for the 6th instance
            val hostView6 = instance6.getRippleHostView()

            // It should now be a totally new host view, not previously used by any of the other
            // instances, since there are no more unused views
            Truth.assertThat(hostView6).isNotEqualTo(hostView1)
            Truth.assertThat(hostView6).isNotEqualTo(hostView2)
            Truth.assertThat(hostView6).isNotEqualTo(hostView3)
            Truth.assertThat(hostView6).isNotEqualTo(hostView4)
            Truth.assertThat(hostView6).isNotEqualTo(hostView5)
        }
    }
}

private fun createRippleIndicationInstance(container: RippleContainer) =
    AndroidRippleIndicationInstance(
        true,
        Dp.Unspecified,
        mutableStateOf(Color.Black),
        mutableStateOf(RippleAlpha(1f, 1f, 1f, 1f)),
        container
    )
