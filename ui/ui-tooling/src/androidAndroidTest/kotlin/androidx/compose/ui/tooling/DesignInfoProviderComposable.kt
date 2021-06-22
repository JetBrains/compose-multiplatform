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

package androidx.compose.ui.tooling

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DesignInfoProviderA() {
    designInfoWithCustomArgument("A", "ObjectA")
}

@Composable
fun DesignInfoProviderB() {
    designInfoWithCustomArgument("B", "ObjectB")
}

@Preview
@Composable
fun ScaffoldDesignInfoProvider() {
    Scaffold {
        DesignInfoProviderA()
    }
}

@Composable
private fun designInfoWithCustomArgument(expectedArgumentValue: String, response: String) =
    remember {
        object : Any() {
            fun getDesignInfo(x: Int, y: Int, args: String) = run {
                val receivedCoordinates = "x=$x, y=$y"
                if (args == expectedArgumentValue) {
                    "$response, $receivedCoordinates"
                } else {
                    "Invalid, $receivedCoordinates"
                }
            }
        }
    }