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

package androidx.compose.mock

fun MockViewComposition.point(point: Point) {
    text("X: ${point.x} Y: ${point.y}")
}

fun MockViewValidator.point(point: Point) {
    text("X: ${point.x} Y: ${point.y}")
}

object SLPoints

fun MockViewComposition.points(points: Iterable<Point>) {
    repeat(of = points) {
        memoize(SLPoints, it) { point(it) }
    }
}

fun MockViewValidator.points(points: Iterable<Point>) {
    repeat(of = points) {
        point(it)
    }
}
