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

package androidx.compose.runtime

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DisposeTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    private val NeverEqualObject = object {
        override fun equals(other: Any?): Boolean {
            return false
        }
    }

    @Test
    fun testDisposeComposition() {
        val log = mutableListOf<String>()

        lateinit var recomposeScope: RecomposeScope
        val composable = @Composable {
            recomposeScope = currentRecomposeScope
            DisposableEffect(NeverEqualObject) {
                log.add("onCommit")
                onDispose {
                    log.add("onCommitDispose")
                }
            }
            DisposableEffect(Unit) {
                log.add("onActive")
                onDispose {
                    log.add("onActiveDispose")
                }
            }
        }

        fun assertLog(expected: String, block: () -> Unit) {
            log.clear()
            block()
            TestCase.assertEquals(expected, log.joinToString())
        }

        assertLog("onCommit, onActive") {
            activity.show(composable)
            activity.waitForAFrame()
        }

        assertLog("onCommitDispose, onCommit") {
            recomposeScope.invalidate()
            activity.waitForAFrame()
        }

        assertLog("onActiveDispose, onCommitDispose") {
            activity.uiThread {
                activity.setContentView(View(activity))
            }
            activity.waitForAFrame()
        }

        assertLog("onCommit, onActive") {
            activity.show(composable)
            activity.waitForAFrame()
        }
    }
}