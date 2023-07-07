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

package androidx.compose.foundation.selection

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics

/**
 * Use this modifier to group a list of [selectable] items
 * like Tabs or RadioButtons together for accessibility purpose.
 *
 * @see selectableGroup
 */
fun Modifier.selectableGroup() = this.semantics {
    selectableGroup()
}