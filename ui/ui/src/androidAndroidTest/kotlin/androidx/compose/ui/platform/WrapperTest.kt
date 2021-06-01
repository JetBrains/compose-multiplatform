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
package androidx.compose.ui.platform

import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Composable private fun Recompose(body: @Composable (recompose: () -> Unit) -> Unit) {
    val scope = currentRecomposeScope
    body { scope.invalidate() }
}

@MediumTest
@RunWith(AndroidJUnit4::class)
class WrapperTest {

    lateinit var activityScenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        activityScenario = ActivityScenario.launch(TestActivity::class.java)
        // Default Recomposer will not recompose if the lifecycle state is not at least STARTED
        activityScenario.moveToState(Lifecycle.State.STARTED)
    }

    @Test
    fun ensureComposeWrapperDoesntPropagateInvalidations() {
        val commitLatch = CountDownLatch(2)
        var composeWrapperCount = 0
        var innerCount = 0

        activityScenario.onActivity {
            it.setContent {
                SideEffect { composeWrapperCount++ }
                Recompose { recompose ->
                    SideEffect {
                        innerCount++
                        commitLatch.countDown()
                    }
                    DisposableEffect(Unit) {
                        recompose()
                        onDispose { }
                    }
                }
            }
        }
        assertTrue(commitLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, composeWrapperCount)
        assertEquals(2, innerCount)
    }

    @Test
    fun disposedWhenActivityDestroyed() {
        val composedLatch = CountDownLatch(1)
        val disposeLatch = CountDownLatch(1)

        lateinit var owner: RegistryOwner
        activityScenario.onActivity {
            owner = RegistryOwner()

            val view = ComposeView(it)
            it.setContentView(view)
            ViewTreeLifecycleOwner.set(view, owner)
            view.setContent {
                DisposableEffect(Unit) {
                    onDispose {
                        disposeLatch.countDown()
                    }
                }
                composedLatch.countDown()
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        activityScenario.onActivity {
            assertEquals(1, disposeLatch.count)
            owner.registry.currentState = Lifecycle.State.DESTROYED
        }

        assertTrue(disposeLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun detachedFromLifecycleWhenDisposed() {
        lateinit var owner: RegistryOwner
        val composedLatch = CountDownLatch(1)
        lateinit var view: ComposeView
        activityScenario.onActivity {
            owner = RegistryOwner()
            view = ComposeView(it)
            it.setContentView(
                // Wrap the ComposeView in a FrameLayout to be the content view;
                // the default recomposer factory will install itself at the content view
                // and use the available ViewTreeLifecycleOwner there. The added layer of
                // nesting here isolates *only* the ComposeView's lifecycle observation.
                FrameLayout(it).apply {
                    addView(view)
                }
            )
            ViewTreeLifecycleOwner.set(view, owner)
            view.setContent {
                composedLatch.countDown()
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        activityScenario.onActivity {
            assertEquals(2, owner.registry.observerCount)
            view.disposeComposition()
            assertEquals(1, owner.registry.observerCount)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    @Ignore("b/159106722")
    fun compositionLinked_whenParentProvided() {
        val composedLatch = CountDownLatch(1)
        var value = 0f

        activityScenario.onActivity {
            val frameLayout = FrameLayout(it)
            it.setContent {
                val compositionLocal = compositionLocalOf<Float> { error("not set") }
                CompositionLocalProvider(compositionLocal provides 1f) {
                    val composition = rememberCompositionContext()

                    AndroidView({ frameLayout })
                    SideEffect {
                        frameLayout.setContent(composition) {
                            value = compositionLocal.current
                            composedLatch.countDown()
                        }
                    }
                }
            }
        }
        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1f, value)
    }

    @Test
    fun activitySetContentIsSynchronouslyComposing() {
        val activityScenario: ActivityScenario<TestActivity> =
            ActivityScenario.launch(TestActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.CREATED)

        activityScenario.onActivity {
            var composed = false
            it.setContent {
                check(!composed) { "the content is expected to be composed once" }
                composed = true
            }
            assertTrue("setContent didn't compose the content synchronously", composed)
        }
    }

    private class RegistryOwner : LifecycleOwner {
        var registry = LifecycleRegistry(this).also {
            it.currentState = Lifecycle.State.RESUMED
        }
        override fun getLifecycle() = registry
    }
}
