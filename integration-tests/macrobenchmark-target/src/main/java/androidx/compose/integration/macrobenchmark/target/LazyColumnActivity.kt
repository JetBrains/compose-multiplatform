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
import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

class LazyColumnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 3000)
        val entries = List(itemCount) { Entry("Item $it") }

        setContent {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "IamLazy" }
            ) {
                items(entries) {
                    ListRow(it)
                }
            }
        }

        launchIdlenessTracking()
    }

    companion object {
        const val EXTRA_ITEM_COUNT = "ITEM_COUNT"
    }
}

internal fun ComponentActivity.launchIdlenessTracking() {
    val contentView: View = findViewById(android.R.id.content)
    val callback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (Recomposer.runningRecomposers.value.any { it.hasPendingWork }) {
                contentView.contentDescription = "COMPOSE-BUSY"
            } else {
                contentView.contentDescription = "COMPOSE-IDLE"
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    Choreographer.getInstance().postFrameCallback(callback)
}

@Composable
private fun ListRow(entry: Entry) {
    Card(modifier = Modifier.padding(8.dp)) {
        Row {
            Text(
                text = entry.contents,
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

data class Entry(val contents: String)
