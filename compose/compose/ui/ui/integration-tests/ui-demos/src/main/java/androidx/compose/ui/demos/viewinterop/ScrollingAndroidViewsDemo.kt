/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.demos.viewinterop

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.demos.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val ItemCount = 50

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScrollingAndroidViewsDemo() {
    Column {
        var checkedItems by remember { mutableStateOf(emptySet<Int>()) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = { checkedItems = (0 until ItemCount).toSet() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Check All")
            }

            Button(
                onClick = { checkedItems = emptySet() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Uncheck All")
            }
        }

        RecyclingAndroidViewLazyColumn(
            checkedItems = checkedItems,
            onChangeCheck = { item, checked ->
                @Suppress("SuspiciousCollectionReassignment")
                if (checked) checkedItems += item
                else checkedItems -= item
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RecyclingAndroidViewLazyColumn(
    checkedItems: Set<Int>,
    onChangeCheck: (Int, Boolean) -> Unit
) {
    var allocationCounter by remember { mutableStateOf(0) }
    val resetViews = remember { mutableSetOf<View>() }

    LazyColumn {
        items(ItemCount, key = { it }) { index ->
            AndroidView<View>(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    LayoutInflater.from(context)
                        .inflate(R.layout.android_view_row_in_lazy_column, null)
                        .apply {
                            findViewById<TextView>(R.id.android_view_row_allocation_id).text =
                                "View Instance ${allocationCounter++}"

                            val checkBox = findViewById<CheckBox>(R.id.android_view_row_checkbox)
                            setOnClickListener { checkBox.toggle() }
                        }
                },
                update = { view ->
                    view.findViewById<TextView>(R.id.android_view_row_label).text =
                        "Item $index"

                    view.findViewById<CheckBox>(R.id.android_view_row_checkbox).apply {
                        setOnCheckedChangeListener { _, checked ->
                            onChangeCheck(index, checked)
                        }

                        isChecked = index in checkedItems
                        if (view in resetViews) {
                            jumpDrawablesToCurrentState()
                            resetViews -= view
                        }
                    }
                },
                onReset = { view ->
                    resetViews += view
                }
            )
        }
    }
}