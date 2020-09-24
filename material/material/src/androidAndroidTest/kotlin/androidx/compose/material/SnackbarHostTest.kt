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

package androidx.compose.material

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performClick
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterialApi::class)
class SnackbarHostTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun snackbarHost_observePushedData() {
        var resultedInvocation = ""
        val hostState = SnackbarHostState()
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            SnackbarHost(hostState) { data ->
                remember(data) {
                    resultedInvocation += data.message
                    data.dismiss()
                }
            }
        }
        val job = scope.launch {
            hostState.showSnackbar("1")
            Truth.assertThat(resultedInvocation).isEqualTo("1")
            hostState.showSnackbar("2")
            Truth.assertThat(resultedInvocation).isEqualTo("12")
            hostState.showSnackbar("3")
            Truth.assertThat(resultedInvocation).isEqualTo("123")
        }
        runBlocking {
            job.join()
        }
    }

    @Test
    fun snackbarHost_fifoQueueContract() {
        var resultedInvocation = ""
        val hostState = SnackbarHostState()
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            SnackbarHost(hostState) { data ->
                remember(data) {
                    resultedInvocation += data.message
                    scope.launch {
                        delay(30L)
                        data.dismiss()
                    }
                }
            }
        }
        val parent = SupervisorJob()
        repeat(10) {
            scope.launch(parent) {
                delay(it * 10L)
                hostState.showSnackbar(it.toString())
            }
        }
        runBlocking {
            parent.children.forEach { it.join() }
        }
        Truth.assertThat(resultedInvocation).isEqualTo("0123456789")
    }

    @Test
    @LargeTest
    fun snackbarHost_returnedResult() {
        val hostState = SnackbarHostState()
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            SnackbarHost(hostState) { data ->
                Snackbar(data)
            }
        }
        val job1 = scope.launch {
            val result = hostState.showSnackbar("1", actionLabel = "press")
            Truth.assertThat(result).isEqualTo(SnackbarResult.ActionPerformed)
        }
        rule.onNodeWithText("press")
            .performClick()
        runBlocking {
            job1.join()
        }
        val job2 = scope.launch {
            val result = hostState.showSnackbar(
                message = "1",
                actionLabel = "do not press"
            )
            Truth.assertThat(result).isEqualTo(SnackbarResult.Dismissed)
        }
        runBlocking {
            job2.join()
        }
    }

    @Test
    fun snackbarHost_scopeLifecycleRespect() {
        val switchState = mutableStateOf(true)
        val hostState = SnackbarHostState()
        lateinit var scope: CoroutineScope
        rule.setContent {
            if (switchState.value) {
                scope = rememberCoroutineScope()
            }
            SnackbarHost(hostState) { data ->
                Snackbar(data)
            }
        }
        val job1 = scope.launch {
            hostState.showSnackbar("1")
            Truth.assertWithMessage("Result shouldn't happen due to cancellation").fail()
        }
        val job2 = scope.launch {
            delay(10)
            switchState.value = false
        }
        runBlocking {
            job2.join()
            job1.join()
        }
    }
}