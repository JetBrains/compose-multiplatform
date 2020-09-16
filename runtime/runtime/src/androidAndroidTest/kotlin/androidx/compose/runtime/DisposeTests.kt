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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DisposeTests : BaseComposeTest() {
    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testDisposeComposition() {
        val log = mutableListOf<String>()

        @OptIn(ExperimentalComposeApi::class)
        val composable = @Composable @ComposableContract(tracked = false) {
            onCommit {
                log.add("onCommit")
                onDispose {
                    log.add("onCommitDispose")
                }
            }
            onActive {
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

        var composition: Composition? = null

        assertLog("onCommit, onActive") {
            composition = activity.show(composable)
            activity.waitForAFrame()
        }

        assertLog("onCommitDispose, onCommit") {
            activity.show(composable)
            activity.waitForAFrame()
        }

        assertLog("onActiveDispose, onCommitDispose") {
            activity.uiThread {
                composition?.dispose()
            }
            activity.waitForAFrame()
        }

        assertLog("onCommit, onActive") {
            activity.show(composable)
            activity.waitForAFrame()
        }
    }
}