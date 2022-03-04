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

package androidx.compose.ui.demos.recyclerview

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.demos.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.customview.poolingcontainer.isPoolingContainer
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewInteropDemoFragment : AbstractInteropDemoFragment(true)
class RecyclerViewInteropOffDemoFragment : AbstractInteropDemoFragment(false)

abstract class AbstractInteropDemoFragment(val interopOn: Boolean) :
    Fragment(R.layout.interop_demo) {

    lateinit var recyclerView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = MainAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        if (!interopOn) {
            recyclerView.isPoolingContainer = false
        }
    }

    inner class MainAdapter : RecyclerView.Adapter<MainAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemRow: ComposeItemRow = itemView.findViewById(R.id.itemRow)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(R.layout.interop_demo_row, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemRow.index = position
        }

        override fun getItemCount(): Int = 50
    }
}

@Composable
fun ItemRow(index: Int) {
    DisposableEffect(Unit) {
        println("ItemRow $index composed")

        onDispose { println("ItemRow $index DISPOSED") }
    }
    Column(Modifier.fillMaxWidth()) {
        Text("Row #${index + 1}", Modifier.padding(horizontal = 8.dp))
        LazyRow {
            items(25) { colIdx ->
                Column(Modifier.padding(8.dp).size(96.dp, 144.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(0.75f)
                            .background(Color(0xFF999999))
                    )
                    Text("Item #$colIdx")
                }
            }
        }
    }
}

class ComposeItemRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
    var index by mutableStateOf(0)

    @Composable
    override fun Content() {
        key(index) {
            ItemRow(index)
        }
    }
}
