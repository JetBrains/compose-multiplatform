/*
 * Copyright 2022 The Android Open Source Project
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

import kotlin.test.Test
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate

// Do not upstream this file:
// These tests don't test runtime, but they rather test compiler plugin (mostly for kjs).
// Ideally such tests need to be in compiler plugin module. But it's easier to have them here now.
class InlineCases {

    @Composable
    private inline fun inlineComposable(content: @Composable () -> Unit) {
        content()
    }

    @Composable
    private fun SimpleCase() {
        inlineComposable {
            Text("Abc")
        }
    }

    @Test
    fun testSimpleCase() = compositionTest {
        compose {
            SimpleCase()
        }

        validate {
            Text("Abc")
        }
    }

    @Composable
    private fun LambdaSavedAsVal() {
        val lambda = @Composable {
            Text("Abc")
        }
        inlineComposable(lambda)
    }

    @Test
    fun testLambdaSavedAsVal() = compositionTest {
        compose {
            LambdaSavedAsVal()
        }

        validate {
            Text("Abc")
        }
    }


    private fun retComposableLambda(): @Composable () -> Unit {
        return { Text("Abc") }
    }

    @Composable
    private fun LambdaReturnedFromFun() {
        inlineComposable(retComposableLambda())
    }

    @Test
    fun testLambdaReturnedFromFun() = compositionTest {
        compose {
            LambdaReturnedFromFun()
        }

        validate {
            Text("Abc")
        }
    }
}