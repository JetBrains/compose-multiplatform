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

internal class DesktopPathMeasure : PathMeasure {
    private val skija = org.jetbrains.skija.PathMeasure()

    override fun setPath(path: Path?, forceClosed: Boolean) {
        skija.setPath(path?.asDesktopPath(), forceClosed)
    }

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ) = skija.getSegment(
        startDistance,
        stopDistance,
        destination.asDesktopPath(),
        startWithMoveTo
    )

    override val length: Float
        get() = skija.length
}

actual fun PathMeasure(): PathMeasure =
    DesktopPathMeasure()