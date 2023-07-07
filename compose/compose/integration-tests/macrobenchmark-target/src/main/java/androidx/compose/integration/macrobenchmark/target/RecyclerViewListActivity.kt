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
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * This activity uses ComposeViews inside a RecyclerView. This helps us benchmark
 * a common point of migration between views and Compose. The implementation is designed to
 * match the LazyColumnActivity and the RecyclerViewActivity for comparison.
 */
class RecyclerViewListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recyclerview)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 3000)

        val adapter = EntryAdapter(entries(itemCount))
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        launchIdlenessTracking()
    }

    private fun entries(size: Int) = List(size) {
        Entry("Item $it")
    }

    companion object {
        const val EXTRA_ITEM_COUNT = "ITEM_COUNT"
    }
}

class EntryViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }

    fun bind(entry: Entry) {
        composeView.setContent {
            MaterialTheme {
                ListRow(entry)
            }
        }
    }
}

class EntryAdapter(
    private val entries: List<Entry>,
) : RecyclerView.Adapter<EntryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        return EntryViewHolder(ComposeView(parent.context))
    }

    override fun onViewRecycled(holder: EntryViewHolder) {
        // Dispose the underlying Composition of the ComposeView
        // when RecyclerView has recycled this ViewHolder
        holder.composeView.disposeComposition()
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
    }

    override fun getItemCount(): Int = entries.size
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