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

package androidx.compose.runtime.rxjava3

import androidx.test.filters.MediumTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class CompletableAdapterTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun whenNotCompletedSetWeGotFalse() {
        val stream = CompletableStream()
        var realValue: Boolean? = null
        rule.setContent {
            realValue = stream.completable.subscribeAsState().value
        }

        Truth.assertThat(realValue).isFalse()
    }

    @Test
    fun weGotTrueWhenWeSubscribeToCompleted() {
        val stream = CompletableStream()
        stream.onComplete()
        var realValue: Boolean? = null
        rule.setContent {
            realValue = stream.completable.subscribeAsState().value
        }

        Truth.assertThat(realValue).isTrue()
    }

    @Test
    fun weGotTrueWhenWeCompleteAfterSubscribing() {
        val stream = CompletableStream()

        var realValue: Boolean? = null
        rule.setContent {
            realValue = stream.completable.subscribeAsState().value
        }

        rule.runOnIdle {
            stream.onComplete()
        }

        rule.runOnIdle {
            Truth.assertThat(realValue).isTrue()
        }
    }
}

private class CompletableStream {

    private val emitters = mutableListOf<CompletableEmitter>()

    val completable = Completable.create {
        if (completed) {
            it.onComplete()
        } else {
            emitters.add(it)
        }
    }

    private var completed = false

    fun onComplete() {
        require(!completed)
        completed = true
        emitters.forEach { if (!it.isDisposed) it.onComplete() }
    }
}