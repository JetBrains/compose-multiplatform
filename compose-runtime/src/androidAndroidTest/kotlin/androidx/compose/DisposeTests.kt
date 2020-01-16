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

package androidx.compose

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisposeTests: BaseComposeTest() {
    @After
    fun teardown() {
        Compose.clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testDisposeComposition() {
        val log = mutableListOf<String>()

        val composable = @Composable {
            val cc = composer
            cc.call(1, { true }) {
                onPreCommit {
                    log.add("onPreCommit")
                    onDispose {
                        log.add("onPreCommitDispose")
                    }
                }
            }
            cc.call(2, { true }) {
                onActive {
                    log.add("onActive")
                    onDispose {
                        log.add("onActiveDispose")
                    }
                }
            }
        }

        fun assertLog(expected: String, block: () -> Unit) {
            log.clear()
            block()
            TestCase.assertEquals(expected, log.joinToString())
        }

        assertLog("onPreCommit, onActive") {
            activity.show(composable)
            activity.waitForAFrame()
        }

        assertLog("onPreCommitDispose, onPreCommit") {
            activity.show(composable)
            activity.waitForAFrame()
        }

        assertLog("onActiveDispose, onPreCommitDispose") {
            activity.disposeTestComposition()
            activity.waitForAFrame()
        }

        assertLog("onPreCommit, onActive") {
            activity.show(composable)
            activity.waitForAFrame()
        }
    }
}