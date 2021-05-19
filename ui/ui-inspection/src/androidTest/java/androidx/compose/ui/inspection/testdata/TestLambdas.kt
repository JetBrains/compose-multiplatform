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

package androidx.compose.ui.inspection.testdata

object TestLambdas {
    val short = { s: String -> s.length }
    val long = { a: Int, b: Int ->
        val sum = a + b
        val count = a - b
        sum / count
    }
    val inlined = { a: Int, b: Int ->
        val sum = a + fct(b) { it * it }
        sum - a
    }

    /**
     * This inline function will appear at a line numbers
     * past the end of this file for JVMTI.
     */
    private inline fun fct(n: Int, op: (Int) -> Int): Int {
        val a = op(n)
        val b = n * a + 3
        return b - a
    }
}
