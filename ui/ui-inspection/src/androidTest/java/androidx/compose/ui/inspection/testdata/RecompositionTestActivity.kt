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

package androidx.compose.ui.inspection.testdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class RecompositionTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                Row {
                    val clickCount1 = remember { mutableStateOf(0) }
                    Column {
                        Button(
                            onClick = { clickCount1.value = clickCount1.value + 1 },
                            modifier = Modifier.padding(16.dp, 4.dp)
                        ) {
                            Text("Click row 1")
                        }
                    }
                    Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                        Text("Row 1 click count: ${clickCount1.value}")
                    }
                }
                Row {
                    val clickCount2 = remember { mutableStateOf(0) }
                    Column {
                        Button(
                            onClick = { clickCount2.value = clickCount2.value + 1 },
                            modifier = Modifier.padding(16.dp, 4.dp)
                        ) {
                            Text("Click row 2")
                        }
                    }
                    Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                        Text("Row 2 click count: ${clickCount2.value}")
                    }
                }
            }
        }
    }
}
