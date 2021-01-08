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
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.dispatch.AndroidUiDispatcher
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference

@LargeTest
@RunWith(AndroidJUnit4::class)
class WindowRecomposerTest {

    /**
     * Test that a Recomposer that doesn't shut down with the activity doesn't inadvertently
     * keep a reference to the Activity
     */
    @OptIn(ExperimentalCoroutinesApi::class, InternalComposeUiApi::class)
    @Test
    @LargeTest
    fun activityGarbageCollected() {
        val localRecomposer = Recomposer(AndroidUiDispatcher.Main)
        val recomposerJob = GlobalScope.launch(AndroidUiDispatcher.Main) {
            localRecomposer.runRecomposeAndApplyChanges()
        }
        lateinit var weakActivityRef: WeakReference<Activity>
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    weakActivityRef = WeakReference(activity)
                    WindowRecomposerPolicy.setFactory { localRecomposer }
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
        } finally {
            // TODO: Change this to the `with` API from a later CL
            @Suppress("DEPRECATION")
            WindowRecomposerPolicy.setFactory(WindowRecomposerFactory.Global)
            localRecomposer.shutDown()
            runBlocking {
                recomposerJob.join()
            }
        }
    }
}