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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DifferentTypesListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 3000)

        setContent {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "IamLazy" }
            ) {
                items(count = itemCount, key = { it }, contentType = { it % 2 }) {
                    if (it % 2 == 0) {
                        EvenItem(it)
                    } else {
                        OddItem(it)
                    }
                }
            }
        }

        launchIdlenessTracking()
    }

    companion object {
        const val EXTRA_ITEM_COUNT = "ITEM_COUNT"
    }
}

@Composable
private fun OddItem(index: Int) {
    Card(modifier = Modifier.padding(8.dp)) {
        Row {
            Text(
                text = "Odd item $index",
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.weight(1f, fill = true))
            Checkbox(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun EvenItem(index: Int) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Even item title $index", fontSize = 17.sp)
        Text(text = "Even item description")
    }
}
