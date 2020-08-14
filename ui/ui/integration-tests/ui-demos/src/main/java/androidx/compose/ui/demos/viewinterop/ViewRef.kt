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

package androidx.compose.ui.demos.viewinterop

import android.view.View
import androidx.compose.ui.node.Ref

// TODO(popam): this file is a subset of ViewRef.kt in androidview. Deduplicate later

/**
 * A Ref is essentially a "value-holder" class that can be used with Compose to get
 * controlled access to the underlying view instances that are constructed as a result
 * of a compose() pass in Compose.
 *
 * See [ViewInteropDemo] for an example.
 */
fun <T : View> T.setRef(ref: Ref<T>) {
    storedRef?.value = null
    storedRef = ref
    ref.value = this
}

internal var <T : View> T.storedRef: Ref<T>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return getTag(refKey) as? Ref<T>
    }
    set(value) {
        setTag(refKey, value)
    }

private val refKey = (3 shl 24) or "Ref".hashCode()
