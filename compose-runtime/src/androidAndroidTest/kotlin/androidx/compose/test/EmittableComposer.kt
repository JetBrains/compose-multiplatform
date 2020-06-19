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
package androidx.compose.test

import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import androidx.compose.Composable
import androidx.ui.viewinterop.emitView

@Composable
fun TextView(
    id: Int = 0,
    text: String = "",
    onClickListener: View.OnClickListener? = null
) {
    emitView(::TextView) {
        if (id != 0) it.id = id
        it.text = text
        if (onClickListener != null) it.setOnClickListener(onClickListener)
    }
}

@Composable
fun Button(
    id: Int = 0,
    text: String = "",
    onClickListener: View.OnClickListener? = null
) {
    emitView(::Button) {
        if (id != 0) it.id = id
        it.text = text
        if (onClickListener != null) it.setOnClickListener(onClickListener)
    }
}

@Composable
fun LinearLayout(
    id: Int = 0,
    orientation: Int = LinearLayout.VERTICAL,
    onClickListener: View.OnClickListener? = null,
    children: @Composable () -> Unit
) {
    emitView(
        ::LinearLayout,
        {
            if (id != 0) it.id = id
            if (onClickListener != null) it.setOnClickListener(onClickListener)
            it.orientation = orientation
        },
        children
    )
}
