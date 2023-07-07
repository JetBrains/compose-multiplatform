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

package androidx.compose.ui.demos.viewinterop

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.sp

@Composable
fun FocusTransferDemo() {
    AndroidView(
        factory = {
            RecyclerView(it).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(it, VERTICAL, false)
                adapter = DemoAdapter(listOf("purple", "blue", "green", "yellow", "orange", "red"))
            }
        },
        update = { it.requestFocus() }
    )
}

private class DemoAdapter(val entries: List<String>) : Adapter<DemoAdapter.DemoViewHolder>() {
    class DemoViewHolder(val text: MutableState<String>, item: View) : ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
        val text = mutableStateOf("")
        return DemoViewHolder(
            text,
            ComposeView(parent.context).apply {
                this.isFocusable = true
                this.isFocusableInTouchMode = true
                val focusRequester = FocusRequester()

                this.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        focusRequester.requestFocus()
                    }
                }

                setContent {
                    FocusableText(
                        text = text.value,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            }
        )
    }

    override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
        holder.text.value = entries.elementAt(position)
    }

    override fun getItemCount(): Int = entries.size
}

@Composable
private fun FocusableText(text: String, modifier: Modifier) {
    var color by remember { mutableStateOf(Color.Unspecified) }
    val focusRequester = remember { FocusRequester() }
    Text(
        modifier = modifier
            .background(color)
            .focusRequester(focusRequester)
            .onFocusEvent { color = if (it.isFocused) Color.LightGray else Color.Unspecified }
            .focusTarget()
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        text = text,
        fontSize = 30.sp
    )
}
