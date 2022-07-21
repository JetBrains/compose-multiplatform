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
package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.graphics.internal.JvmDefaultWithCompatibility

/**
 * Receiver scope for drawing content into a layout, where the content can
 * be drawn between other canvas operations. If [drawContent] is not called,
 * the contents of the layout will not be drawn.
 */
@JvmDefaultWithCompatibility
interface ContentDrawScope : DrawScope {
    /**
     * Causes child drawing operations to run during the `onPaint` lambda.
     */
    fun drawContent()
}
