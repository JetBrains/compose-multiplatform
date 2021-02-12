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

package androidx.compose.ui.tooling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.ui.platform.LocalInspectionMode
import java.util.Collections
import java.util.WeakHashMap

/**
 * Storage for the preview generated [CompositionData]s.
 */
internal interface CompositionDataRecord {
    val store: Set<CompositionData>

    companion object {
        fun create(): CompositionDataRecord = CompositionDataRecordImpl()
    }
}

private class CompositionDataRecordImpl : CompositionDataRecord {
    @OptIn(InternalComposeApi::class)
    override val store: MutableSet<CompositionData> =
        Collections.newSetFromMap(WeakHashMap())
}

/**
 * A wrapper for compositions in inspection mode. The composition inside the Inspectable component
 * is in inspection mode.
 *
 * @param compositionDataRecord [CompositionDataRecord] to record the SlotTable used in the
 * composition of [content]
 *
 * @suppress
 */
@Composable
@OptIn(InternalComposeApi::class)
internal fun Inspectable(
    compositionDataRecord: CompositionDataRecord,
    content: @Composable () -> Unit
) {
    currentComposer.collectParameterInformation()
    val store = (compositionDataRecord as CompositionDataRecordImpl).store
    store.add(currentComposer.compositionData)
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalInspectionTables provides store,
        content = content
    )
}

/**
 * A wrapper for inspection-mode-only behavior. The children of this component will only be included
 * in the composition when the composition is in inspection mode.
 */
@Composable
fun InInspectionModeOnly(content: @Composable () -> Unit) {
    if (LocalInspectionMode.current) {
        content()
    }
}
