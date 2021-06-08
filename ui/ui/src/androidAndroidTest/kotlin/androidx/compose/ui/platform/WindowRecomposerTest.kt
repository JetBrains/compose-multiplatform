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

package androidx.compose.ui.platform

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.core.view.get
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference

@RunWith(AndroidJUnit4::class)
class WindowRecomposerTest {

    /**
     * Test that a Recomposer that doesn't shut down with the activity doesn't inadvertently
     * keep a reference to the Activity
     */
    @kotlin.OptIn(DelicateCoroutinesApi::class, InternalComposeUiApi::class)
    @Test
    @LargeTest
    fun activityGarbageCollected() {
        val localRecomposer = Recomposer(AndroidUiDispatcher.Main)
        val recomposerJob = GlobalScope.launch(AndroidUiDispatcher.Main) {
            localRecomposer.runRecomposeAndApplyChanges()
        }
        lateinit var weakActivityRef: WeakReference<Activity>
        try {
            WindowRecomposerPolicy.withFactory({ localRecomposer }) {
                ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                    scenario.onActivity { activity ->
                        weakActivityRef = WeakReference(activity)
                        activity.setContentView(
                            ComposeView(activity).apply {
                                setContent {
                                    Box(Modifier.background(Color.Blue).fillMaxSize())
                                }
                            }
                        )
                    }
                    assertNotNull(weakActivityRef.get())
                }
                repeat(10) {
                    Runtime.getRuntime().gc()
                }
                assertNull("expected Activity to have been collected", weakActivityRef.get())
            }
        } finally {
            localRecomposer.cancel()
            runBlocking {
                recomposerJob.join()
            }
        }
    }

    /**
     * The Android framework may reuse the window decor views in some cases of activity
     * recreation for configuration changes, notably during dynamic window resizing in
     * multi-window modes. Confirm that the [windowRecomposer] extension returns a recomposer
     * based in the content views, not in the decor itself, as this can cause a recomposer to
     * become decoupled from its `DESTROYED` host lifecycle - the old Activity instance.
     *
     * Regression test for https://issuetracker.google.com/issues/184293033
     */
    @Test
    @MediumTest
    fun windowRecomposerResetsWithContentChild() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            var firstRecomposer: Recomposer? = null
            scenario.onActivity {
                it.setContent {
                    BasicText("Hello, world")
                }
                val contentParent = it.findViewById<ViewGroup>(android.R.id.content)
                assertEquals("child count of @android:id/content", 1, contentParent.childCount)
                firstRecomposer = contentParent[0].windowRecomposer
            }

            var secondRecomposer: Recomposer? = null
            scenario.onActivity {
                // force removal of the old composition host view and don't reuse
                it.setContentView(View(it))
                it.setContent {
                    BasicText("Hello, again!")
                }
                val contentParent = it.findViewById<ViewGroup>(android.R.id.content)
                assertEquals("child count of @android:id/content", 1, contentParent.childCount)
                secondRecomposer = contentParent[0].windowRecomposer
            }

            assertNotNull("first recomposer", firstRecomposer)
            assertNotNull("second recomposer", secondRecomposer)
            assertNotSame(firstRecomposer, secondRecomposer)
        }
    }
}