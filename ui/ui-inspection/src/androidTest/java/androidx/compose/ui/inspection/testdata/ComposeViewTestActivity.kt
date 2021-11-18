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

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView

class ComposeViewTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                Text("one")
                AndroidView({ context ->
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(context).apply { text = "AndroidView" })
                        addView(ComposeView(context).apply {
                            setContent {
                                Column {
                                    Text("two")
                                    AndroidView({ context ->
                                        LinearLayout(context).apply {
                                            orientation = LinearLayout.VERTICAL
                                            addView(ComposeView(context).apply {
                                                setContent {
                                                    Text("three")
                                                }
                                            })
                                        }
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }
    }
}
