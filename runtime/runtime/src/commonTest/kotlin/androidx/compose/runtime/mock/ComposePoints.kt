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

package androidx.compose.runtime.mock

import androidx.compose.runtime.Composable

@Composable
fun Point(point: Point) {
    Text("X: ${point.x} Y: ${point.y}")
}

fun MockViewValidator.Point(point: Point) {
    Text("X: ${point.x} Y: ${point.y}")
}

private const val SLPoints = 100

@Composable
fun Points(points: Iterable<Point>) {
    Repeated(of = points) {
        Point(it)
    }
}

fun MockViewValidator.Points(points: Iterable<Point>) {
    Repeated(of = points) {
        Point(it)
    }
}
