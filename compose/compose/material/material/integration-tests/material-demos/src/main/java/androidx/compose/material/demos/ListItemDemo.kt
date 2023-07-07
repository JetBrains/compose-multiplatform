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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.samples.ClickableListItems
import androidx.compose.material.samples.OneLineListItems
import androidx.compose.material.samples.OneLineRtlLtrListItems
import androidx.compose.material.samples.ThreeLineListItems
import androidx.compose.material.samples.ThreeLineRtlLtrListItems
import androidx.compose.material.samples.TwoLineListItems
import androidx.compose.material.samples.TwoLineRtlLtrListItems
import androidx.compose.runtime.Composable

@Composable
fun ListItemDemo() {
    LazyColumn {
        item {
            ClickableListItems()
        }
        item {
            OneLineListItems()
        }
        item {
            TwoLineListItems()
        }
        item {
            ThreeLineListItems()
        }
    }
}

@Composable
fun MixedRtlLtrListItemDemo() {
    LazyColumn {
        item {
            OneLineRtlLtrListItems()
        }
        item {
            TwoLineRtlLtrListItems()
        }
        item {
            ThreeLineRtlLtrListItems()
        }
    }
}
