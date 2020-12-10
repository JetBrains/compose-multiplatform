/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material.demos

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.samples.ClickableListItems
import androidx.compose.material.samples.OneLineListItems
import androidx.compose.material.samples.OneLineRtlLtrListItems
import androidx.compose.material.samples.ThreeLineListItems
import androidx.compose.material.samples.ThreeLineRtlLtrListItems
import androidx.compose.material.samples.TwoLineListItems
import androidx.compose.material.samples.TwoLineRtlLtrListItems
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.imageResource

@Composable
fun ListItemDemo() {
    val icon24 = imageResource(R.drawable.ic_bluetooth)
    val icon40 = imageResource(R.drawable.ic_account_box)
    val icon56 = imageResource(R.drawable.ic_android)
    val vectorIcon = Icons.Default.Call
    ScrollableColumn {
        ClickableListItems()
        OneLineListItems(icon24, icon40, icon56, vectorIcon)
        TwoLineListItems(icon24, icon40)
        ThreeLineListItems(icon24, vectorIcon)
    }
}

@Composable
fun MixedRtlLtrListItemDemo() {
    val icon24 = imageResource(R.drawable.ic_bluetooth)
    val icon40 = imageResource(R.drawable.ic_account_box)
    ScrollableColumn {
        OneLineRtlLtrListItems(icon24, icon40)
        TwoLineRtlLtrListItems(icon40)
        ThreeLineRtlLtrListItems(icon40)
    }
}
