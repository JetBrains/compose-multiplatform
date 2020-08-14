/*
 * Copyright 2018 The Android Open Source Project
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
 * Strategies for combining paths.
 *
 * See also:
 *
 * * [Path.combine], which uses this enum to decide how to combine two paths.
 */
// Must be kept in sync with SkPathOp
enum class PathOperation {
    /**
     * Subtract the second path from the first path.
     *
     * For example, if the two paths are overlapping circles of equal diameter
     * but differing centers, the result would be a crescent portion of the
     * first circle that was not overlapped by the second circle.
     *
     * See also:
     *
     *  * [reverseDifference], which is the same but subtracting the first path
     *    from the second.
     */
    difference,
    /**
     * Create a new path that is the intersection of the two paths, leaving the
     * overlapping pieces of the path.
     *
     * For example, if the two paths are overlapping circles of equal diameter
     * but differing centers, the result would be only the overlapping portion
     * of the two circles.
     *
     * See also:
     *  * [xor], which is the inverse of this operation
     */
    intersect,
    /**
     * Create a new path that is the union (inclusive-or) of the two paths.
     *
     * For example, if the two paths are overlapping circles of equal diameter
     * but differing centers, the result would be a figure-eight like shape
     * matching the outer boundaries of both circles.
     */
    union,
    /**
     * Create a new path that is the exclusive-or of the two paths, leaving
     * everything but the overlapping pieces of the path.
     *
     * For example, if the two paths are overlapping circles of equal diameter
     * but differing centers, the figure-eight like shape less the overlapping parts
     *
     * See also:
     *  * [intersect], which is the inverse of this operation
     */
    xor,
    /**
     * Subtract the first path from the second path.
     *
     * For example, if the two paths are overlapping circles of equal diameter
     * but differing centers, the result would be a crescent portion of the
     * second circle that was not overlapped by the first circle.
     *
     * See also:
     *
     *  * [difference], which is the same but subtracting the second path
     *    from the first.
     */
    reverseDifference
}