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

/**
 * Convert the [org.jetbrains.skia.PathMeasure] instance into a Compose-compatible PathMeasure
 */
fun org.jetbrains.skia.PathMeasure.asComposePathEffect(): PathMeasure = SkiaBackedPathMeasure(this)

/**
 * Obtain a reference to skia PathMeasure type
 */
fun PathMeasure.asSkiaPathMeasure(): org.jetbrains.skia.PathMeasure =
    (this as SkiaBackedPathMeasure).skia

internal class SkiaBackedPathMeasure(
    internal val skia: org.jetbrains.skia.PathMeasure = org.jetbrains.skia.PathMeasure()
) : PathMeasure {

    override fun setPath(path: Path?, forceClosed: Boolean) {
        skia.setPath(path?.asSkiaPath(), forceClosed)
    }

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ) = skia.getSegment(
        startDistance,
        stopDistance,
        destination.asSkiaPath(),
        startWithMoveTo
    )

    override val length: Float
        get() = skia.length
}

actual fun PathMeasure(): PathMeasure =
    SkiaBackedPathMeasure()