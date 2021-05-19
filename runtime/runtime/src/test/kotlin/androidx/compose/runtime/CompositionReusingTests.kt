/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.NonReusableLinear
import androidx.compose.runtime.mock.NonReusableText
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.flatten
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Stable
class CompositionReusingTests {

    @Test
    fun canReuse() = compositionTest {
        var lastData: State<Int>? = null
        var key by mutableStateOf(0)

        compose {
            ReusableContent(key) {
                Linear {
                    val data = remember { mutableStateOf(1) }
                    lastData = data
                    Text("Key = $key")
                }
            }
        }

        validate {
            Linear {
                Text("Key = $key")
            }
        }

        val firstData = lastData
        val nodes = root.flatten()

        key++
        expectChanges()
        revalidate()

        val nodesAfterChange = root.flatten()

        // Ensure nodes are reused
        assertArrayEquals(nodes, nodesAfterChange) { "${it.hashCode()}" }

        // Ensure remembers are not reused
        assertNotEquals(
            firstData, lastData,
            "Should not remember values when recycling"
        )
    }

    @Test
    fun canRecycleAroundNonReusable() = compositionTest {
        var key by mutableStateOf(0)

        compose {
            ReusableContent(key) {
                Linear {
                    Text("Key = $key")
                    NonReusableText("Non-recyclable key = $key")
                }
            }
        }

        validate {
            Linear {
                Text("Key = $key")
                Text("Non-recyclable key = $key")
            }
        }

        val recycleText = findTextWith("Key")
        val nonRecycledText = findTextWith("Non-recyclable key")
        key++
        expectChanges()
        revalidate()

        assertEquals(recycleText, findTextWith("Key"), "Expected text to be recycled")
        assertNotEquals(
            nonRecycledText,
            findTextWith("Non-recyclable key"),
            "Expected non-recyclable text to be replaced"
        )
    }

    @Test
    fun recyclableNodesInNonReusableContainerNotRecycled() = compositionTest {
        var key by mutableStateOf(0)

        compose {
            ReusableContent(key) {
                Linear {
                    Linear {
                        Text("Key = $key")
                    }
                    NonReusableLinear {
                        Text("Non-recyclable key = $key")
                    }
                    NonReusableLinear { }
                }
            }
        }

        validate {
            Linear {
                Linear {
                    Text("Key = $key")
                }
                Linear {
                    Text("Non-recyclable key = $key")
                }
                Linear { }
            }
        }

        val recycleText = findTextWith("Key")
        val nonRecycledText = findTextWith("Non-recyclable key")
        key++
        expectChanges()
        revalidate()
        verifyConsistent()

        assertEquals(recycleText, findTextWith("Key"), "Expected text to be recycled")
        assertNotEquals(
            nonRecycledText,
            findTextWith("Non-recyclable key"),
            "Expected non-recyclable text to be replaced"
        )
    }

    @Test
    fun compositeHashCodeReflectsReusableChanges() = compositionTest {
        var key by mutableStateOf(0)
        var lastCompositeHash = 0

        compose {
            ReusableContent(key) {
                Linear {
                    Text("Key = $key")
                    lastCompositeHash = currentCompositeKeyHash
                }
            }
        }

        validate {
            Linear {
                Text("Key = $key")
            }
        }

        val firstCompositeHash = lastCompositeHash
        key++
        expectChanges()
        revalidate()
        assertNotEquals(firstCompositeHash, lastCompositeHash)
    }

    @Test // regression test for b/188567661
    fun compositeHashCodeIsConsistent() = compositionTest {
        var key by mutableStateOf(0)
        var localValue by mutableStateOf(0)
        var lastCompositeHash = 0

        compose {
            ReusableContent(key) {
                Linear {
                    Text("Key = $key: $localValue")
                    lastCompositeHash = currentCompositeKeyHash
                }
            }
        }

        validate {
            Linear {
                Text("Key = $key: $localValue")
            }
        }

        val compositeHashForKey0 = lastCompositeHash

        localValue++
        expectChanges()
        revalidate()
        assertEquals(compositeHashForKey0, lastCompositeHash)

        key++
        expectChanges()
        revalidate()
        val compositeHashForKey1 = lastCompositeHash
        assertNotEquals(compositeHashForKey0, compositeHashForKey1)

        localValue++
        expectChanges()
        revalidate()
        assertEquals(compositeHashForKey1, lastCompositeHash)
    }
}

private fun View.findTextWith(contains: String) =
    find { it.name == "text" && it.text?.contains(contains) == true }
private fun CompositionTestScope.findTextWith(contains: String) = root.findTextWith(contains)

private fun View.find(predicate: (view: View) -> Boolean): View? {
    if (predicate(this)) return this
    for (child in children) {
        val found = child.find(predicate)
        if (found != null) return found
    }
    return null
}