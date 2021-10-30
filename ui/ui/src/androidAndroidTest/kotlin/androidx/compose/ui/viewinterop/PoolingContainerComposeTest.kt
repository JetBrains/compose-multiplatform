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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.test.R
import androidx.customview.poolingcontainer.callPoolingContainerOnRelease
import androidx.customview.poolingcontainer.isPoolingContainer
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PoolingContainerComposeTest {
    @Rule
    @JvmField
    var activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun addToLifecycleContainer_removeFromLifecycleContainer_addToOtherContainer_thenRemove() {
        activityRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.pooling_container_compose_test)
            val lifecycleContainer: ViewGroup = activity.findViewById(R.id.lifecycleContainer)
            val nonLifecycleContainer: ViewGroup = activity.findViewById(R.id.nonLifecycleContainer)
            val composeView = DisposalCountingComposeView(activity)

            lifecycleContainer.isPoolingContainer = true

            assertThat(composeView.compositions).isEqualTo(0)
            assertThat(composeView.disposals).isEqualTo(0)

            lifecycleContainer.addView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Remove from lifecycle container: Composition should not be disposed
            lifecycleContainer.removeView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Add back into hierarchy: no changes expected
            nonLifecycleContainer.addView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Remove from non-lifecycle container: Composition should be disposed
            nonLifecycleContainer.removeView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(1)
        }
    }

    @Test
    fun lifecycleContainerStateSetAfterAttach() {
        activityRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.pooling_container_compose_test)
            val lifecycleContainer: ViewGroup = activity.findViewById(R.id.lifecycleContainer)
            val nonLifecycleContainer: ViewGroup = activity.findViewById(R.id.nonLifecycleContainer)
            val composeView = DisposalCountingComposeView(activity)

            // Set to the opposite
            nonLifecycleContainer.isPoolingContainer = true

            assertThat(composeView.compositions).isEqualTo(0)
            assertThat(composeView.disposals).isEqualTo(0)

            lifecycleContainer.addView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Remove from lifecycle container: Composition should not be disposed
            lifecycleContainer.isPoolingContainer = true
            lifecycleContainer.removeView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Add back into hierarchy: no changes expected
            nonLifecycleContainer.addView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Remove from non-lifecycle container: Composition should be disposed
            nonLifecycleContainer.isPoolingContainer = false
            nonLifecycleContainer.removeView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(1)
        }
    }

    @Test
    fun inLifecycleContainer_releaseCalled_compositionDisposed() {
        activityRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.pooling_container_compose_test)
            val lifecycleContainer: ViewGroup = activity.findViewById(R.id.lifecycleContainer)
            val composeView = DisposalCountingComposeView(activity)

            lifecycleContainer.addView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Release the container and ensure the child composition is disposed
            lifecycleContainer.callPoolingContainerOnRelease()
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(1)

            // Make sure it's not disposed again
            lifecycleContainer.removeView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(1)
        }
    }

    @Test
    fun customStrategy() {
        activityRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.pooling_container_compose_test)
            val lifecycleContainer: ViewGroup = activity.findViewById(R.id.lifecycleContainer)
            val composeView = DisposalCountingComposeView(activity)
            composeView.setViewCompositionStrategy(object : ViewCompositionStrategy {
                override fun installFor(view: AbstractComposeView): () -> Unit {
                    // do nothing
                    return {}
                }
            })

            lifecycleContainer.isPoolingContainer = true

            assertThat(composeView.compositions).isEqualTo(0)
            assertThat(composeView.disposals).isEqualTo(0)

            lifecycleContainer.addView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            // Removed: Composition should not be disposed, since we've changed the default
            lifecycleContainer.removeView(composeView)
            assertThat(composeView.compositions).isEqualTo(1)
            assertThat(composeView.disposals).isEqualTo(0)

            composeView.disposeComposition()
            assertThat(composeView.disposals).isEqualTo(1)
        }
    }

    class DisposalCountingComposeView(context: Context) : AbstractComposeView(context) {
        var compositions = 0
        var disposals = 0

        @Composable
        override fun Content() {
            DisposableEffect(true) {
                compositions++
                onDispose {
                    disposals++
                }
            }
        }
    }
}