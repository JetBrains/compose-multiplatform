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
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.flatten
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

    @Test
    fun reusableContentHostCanDeactivate() = compositionTest {
        var reuseKey by mutableStateOf(0)
        var active by mutableStateOf(true)

        val rememberedState = object : RememberObserver {
            var currentlyRemembered = false
            var rememberCount = 0
            var forgottenCount = 0
            var abandonCount = 0

            override fun toString(): String = "Some text"

            override fun onRemembered() {
                rememberCount++
                currentlyRemembered = true
            }

            override fun onForgotten() {
                forgottenCount++
                currentlyRemembered = false
            }

            override fun onAbandoned() {
                abandonCount++
                currentlyRemembered = false
            }
        }

        compose {
            ReusableContentHost(active) {
                Linear {
                    ReusableContent(reuseKey) {
                        Linear {
                            val state = remember { rememberedState }
                            Text(state.toString())
                        }
                    }
                }
            }
        }

        validate {
            Linear {
                Linear {
                    Text(rememberedState.toString())
                }
            }
        }

        assertTrue(rememberedState.currentlyRemembered)

        active = false
        expectChanges()
        revalidate()
        assertFalse(rememberedState.currentlyRemembered)

        active = true
        expectChanges()
        revalidate()
        assertTrue(rememberedState.currentlyRemembered)

        reuseKey++
        expectChanges()
        revalidate()
        assertTrue(rememberedState.currentlyRemembered)
    }

    @Test
    fun reusableContentHostCanDisableRecompose() = compositionTest {
        var active by mutableStateOf(true)
        var outer by mutableStateOf("Outer")
        var name by mutableStateOf("Value")

        val rememberedState = object : RememberObserver {
            var currentlyRemembered = false
            override fun toString(): String = "Test"
            override fun onRemembered() { currentlyRemembered = true }
            override fun onForgotten() { currentlyRemembered = false }
            override fun onAbandoned() { currentlyRemembered = false }
        }

        compose {
            Text(outer)
            ReusableContentHost(active) {
                Linear {
                    val state = remember { rememberedState }
                    Text("$state $name")
                }
            }
        }

        validate {
            Text(outer)
            Linear {
                Text("$rememberedState $name")
            }
        }

        active = false
        expectChanges()

        name = "New value"

        // Name should not be observed.
        expectNoChanges()

        // Intentionally not calling revalidate() here as the tree that needs updating is disabled
        // and out of sync with the state of name

        outer = "New outer"
        expectChanges()

        // Still not valid yet but should have recomposed

        active = true
        expectChanges()
        revalidate()

        name = "New new value"
        expectChanges()
        revalidate()
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