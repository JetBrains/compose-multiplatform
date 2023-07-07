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
import android.view.LayoutInflater
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

/**
 * This activity uses AndroidViews inside a LazyColumn. This helps us benchmark
 * a common point of migration between views and Compose. The implementation is designed to
 * match the LazyColumnActivity and the RecyclerViewActivity for comparison.
 */
class AndroidViewListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 3000)

        setContent {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "IamLazy" }
            ) {
                items(List(itemCount) { Entry("Item $it") }) {
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

@Composable
private fun ListRow(entry: Entry) {
    AndroidView(
        factory = { context ->
            val layoutInflator = LayoutInflater.from(context)
            layoutInflator.inflate(R.layout.recycler_row, null, false)
        },
        modifier = Modifier.fillMaxWidth()
    ) { view ->
        view.findViewById<AppCompatTextView>(R.id.content).text = entry.contents
    }
}