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

package androidx.compose.ui.gesture.scrollorientationlocking

import androidx.compose.ui.input.pointer.CustomEventDispatcher
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

val c1 = down(1, 0.milliseconds, x = 1f, y = 1f)
val c2 = down(2, 0.milliseconds, x = 2f, y = 2f)

class LockingConfig(
    val changes: List<PointerInputChange>,
    val orientation: Orientation
)

@RunWith(Parameterized::class)
class ScrollOrientationLockerTest(
    private val lockingConfigs: List<LockingConfig>,
    private val inputChanges: List<PointerInputChange>,
    private val inputOrientation: Orientation,
    private val expectedOutput: List<PointerInputChange>
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "lockedConfigs = {0}, " +
                "inputChanges = {1}, " +
                "inputOrientation = {2}, " +
                "expectedOutput = {3}"
        )
        fun parameters(): List<Array<Any?>> {

            val configs = mutableListOf<Array<Any?>>()

            val allPointerCombinations = listOf(
                listOf(),
                listOf(c1),
                listOf(c2),
                listOf(c1, c2)
            )

            val pointerCombinationsWith2 =
                listOf(
                    listOf(c2),
                    listOf(c1, c2)
                )

            // If the requested orientation and the locked orientation is the same, whatever is
            // requested is returned.
            allPointerCombinations.forEach { locked ->
                allPointerCombinations.forEach { input ->
                    Orientation.values().forEach { orientation ->
                        configs.add(
                            arrayOf(
                                listOf(
                                    LockingConfig(locked, orientation)
                                ),
                                input,
                                orientation,
                                input
                            )
                        )
                    }
                }
            }

            // If pointer 1 is locked in orientation A, all requests with sets that include
            // pointer 2 and in orientation B will result in the set that just includes pointer 2.
            pointerCombinationsWith2.forEach { input ->
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1), Orientation.Horizontal)
                        ),
                        input,
                        Orientation.Vertical,
                        listOf(c2)
                    )
                )
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1), Orientation.Vertical)
                        ),
                        input,
                        Orientation.Horizontal,
                        listOf(c2)
                    )
                )
            }

            // If pointer 1 is locked in orientation A, and pointer 2 is locked in orientation B,
            // all requests with sets that include pointer 2 and with orientation B will result in
            // the set that just includes Pointer 2.
            pointerCombinationsWith2.forEach { input ->
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1), Orientation.Horizontal),
                            LockingConfig(listOf(c2), Orientation.Vertical)
                        ),
                        input,
                        Orientation.Vertical,
                        listOf(c2)
                    )
                )
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1), Orientation.Vertical),
                            LockingConfig(listOf(c2), Orientation.Horizontal)
                        ),
                        input,
                        Orientation.Horizontal,
                        listOf(c2)
                    )
                )
            }

            // If all of the changes are locked to one orientation, no matter what is requested
            // in the other, no changes will be returned.
            allPointerCombinations.forEach { input ->
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1, c2), Orientation.Horizontal)
                        ),
                        input,
                        Orientation.Vertical,
                        listOf<PointerInputChange>()
                    )
                )
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1, c2), Orientation.Vertical)
                        ),
                        input,
                        Orientation.Horizontal,
                        listOf<PointerInputChange>()
                    )
                )
            }

            // If pointer 1 is locked in orientation A, and then is attempted to be locked in
            // orientation B, all requests with sets that include pointer 2 and in orientation B
            // will result in the set that just includes pointer 2.
            pointerCombinationsWith2.forEach { input ->
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1), Orientation.Horizontal),
                            LockingConfig(listOf(c1), Orientation.Vertical)
                        ),
                        input,
                        Orientation.Vertical,
                        listOf(c2)
                    )
                )
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1), Orientation.Vertical),
                            LockingConfig(listOf(c1), Orientation.Horizontal)
                        ),
                        input,
                        Orientation.Horizontal,
                        listOf(c2)
                    )
                )
            }

            // If all of the changes are locked to orientation A, and then are attempted to be
            // locked in brientation B , no matter what is requested in orientation B, no changes
            // will be returned.
            allPointerCombinations.forEach { input ->
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1, c2), Orientation.Horizontal),
                            LockingConfig(listOf(c1, c2), Orientation.Vertical)
                        ),
                        input,
                        Orientation.Vertical,
                        listOf<PointerInputChange>()
                    )
                )
                configs.add(
                    arrayOf(
                        listOf(
                            LockingConfig(listOf(c1, c2), Orientation.Vertical),
                            LockingConfig(listOf(c1, c2), Orientation.Horizontal)

                        ),
                        input,
                        Orientation.Horizontal,
                        listOf<PointerInputChange>()
                    )
                )
            }

            return configs
        }
    }

    @Test
    fun test() {

        // Arrange

        val customEventDispatcher: CustomEventDispatcher = mock()
        val scrollOrientationLocker = ScrollOrientationLocker(customEventDispatcher)
        scrollOrientationLocker.onPointerInputSetup(inputChanges, PointerEventPass.Initial)
        lockingConfigs.forEach {
            scrollOrientationLocker.attemptToLockPointers(it.changes, it.orientation)
        }

        // Act
        val actual = scrollOrientationLocker.getPointersFor(inputChanges, inputOrientation)

        // Assert
        Truth.assertThat(expectedOutput).isEqualTo(actual)
    }
}