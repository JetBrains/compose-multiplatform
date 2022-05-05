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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

class TrivialTracingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Foo_BBC27C8E_13A7_4A5F_A735_AFDC433F54C3()
            Baz_609801AB_F5A9_47C3_9405_2E82542F21B8()
        }
    }
}

@Composable
private fun Foo_BBC27C8E_13A7_4A5F_A735_AFDC433F54C3() =
    Bar_4888EA32_ABC5_4550_BA78_1247FEC1AAC9()

@Composable
private fun Bar_4888EA32_ABC5_4550_BA78_1247FEC1AAC9() {
}

@Composable
private fun Baz_609801AB_F5A9_47C3_9405_2E82542F21B8() {
}