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

package androidx.ui.androidview.adapters

import android.util.DisplayMetrics
import android.view.View

internal val View.metrics: DisplayMetrics get() = resources.displayMetrics

/**
 * This function will take in a string and pass back a valid resource identifier for
 * View.setTag(...). We should eventually move this to a resource id that's actually generated via
 * AAPT but doing that in this project is proving to be complicated, so for now I'm just doing this
 * as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}
