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

package androidx.compose.ui.graphics

actual fun PathMeasure(): PathMeasure = AndroidPathMeasure(android.graphics.PathMeasure())

class AndroidPathMeasure internal constructor(
    private val internalPathMeasure: android.graphics.PathMeasure
) : PathMeasure {

    override val length: Float
        get() = internalPathMeasure.length

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ): Boolean {
        return internalPathMeasure.getSegment(
            startDistance,
            stopDistance,
            destination.asAndroidPath(),
            startWithMoveTo
        )
    }

    override fun setPath(path: Path?, forceClosed: Boolean) {
        internalPathMeasure.setPath(path?.asAndroidPath(), forceClosed)
    }
}
