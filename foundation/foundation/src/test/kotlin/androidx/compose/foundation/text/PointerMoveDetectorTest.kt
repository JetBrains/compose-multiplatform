/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.gestures.SuspendingGestureTestUtil
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Correspondence
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PointerMoveDetectorTest {
    @Test
    fun whenSimpleMovement_allMovesAreReported() {
        val actualMoves = mutableListOf<Offset>()
        SuspendingGestureTestUtil {
            detectMoves { actualMoves.add(it) }
        }.executeInComposition {
            down(5f, 5f)
                .moveTo(4f, 4f)
                .moveTo(3f, 3f)
                .moveTo(2f, 2f)
                .moveTo(1f, 1f)
                .up()

            assertThat(actualMoves).hasEqualOffsets(
                listOf(
                    Offset(4f, 4f),
                    Offset(3f, 3f),
                    Offset(2f, 2f),
                    Offset(1f, 1f),
                )
            )
        }
    }

    @Test
    fun whenMultiplePointers_onlyUseFirst() {
        val actualMoves = mutableListOf<Offset>()
        SuspendingGestureTestUtil {
            detectMoves { actualMoves.add(it) }
        }.executeInComposition {
            var m1 = down(5f, 5f)
            var m2 = down(6f, 6f)
            m1 = m1.moveTo(4f, 4f)
            m2 = m2.moveTo(7f, 7f)
            m1 = m1.moveTo(3f, 3f)
            m2 = m2.moveTo(8f, 8f)
            m1 = m1.moveTo(2f, 2f)
            m2 = m2.moveTo(9f, 9f)
            m1.moveTo(1f, 1f)
            m2.moveTo(10f, 10f)
            m1.up()
            m2.up()

            assertThat(actualMoves).hasEqualOffsets(
                listOf(
                    Offset(4f, 4f),
                    Offset(3f, 3f),
                    Offset(2f, 2f),
                    Offset(1f, 1f),
                )
            )
        }
    }

    @Test
    fun whenMultiplePointers_thenFirstReleases_handOffToNextPointer() {
        val actualMoves = mutableListOf<Offset>()
        SuspendingGestureTestUtil {
            detectMoves { actualMoves.add(it) }
        }.executeInComposition {
            var m1 = down(5f, 5f) // ignored because not a move
            m1 = m1.moveTo(4f, 4f) // used
            m1 = m1.moveTo(3f, 3f) // used
            var m2 = down(4f, 4f) // ignored because still tracking m1
            m1 = m1.moveTo(2f, 2f) // used
            m2 = m2.moveTo(3f, 3f) // ignored because still tracking m1
            m1.up() // ignored because not a move
            m2.moveTo(2f, 2f) // ignored because equal to the previous used move
            m2.moveTo(1f, 1f) // used
            m2.up() // ignored because not a move

            assertThat(actualMoves).hasEqualOffsets(
                listOf(
                    Offset(4f, 4f),
                    Offset(3f, 3f),
                    Offset(2f, 2f),
                    Offset(1f, 1f),
                )
            )
        }
    }

    private fun IterableSubject.hasEqualOffsets(expectedMoves: List<Offset>) {
        comparingElementsUsing(offsetCorrespondence)
            .containsExactly(*expectedMoves.toTypedArray())
            .inOrder()
    }

    private val offsetCorrespondence: Correspondence<Offset, Offset> = Correspondence.from(
        { o1, o2 -> o1!!.x == o2!!.x && o1.y == o2.y },
        "has the offset of",
    )
}
