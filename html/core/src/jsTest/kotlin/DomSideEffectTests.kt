package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.dom.clear
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.testutils.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
class DomSideEffectTests {

    @Test
    fun canCreateElementsInDomSideEffect() = runTest {
        composition {
            Div {
                DomSideEffect {
                    it.appendChild(
                        document.createElement("p").also {
                            it.appendChild(document.createTextNode("Hello World!"))
                        }
                    )
                }
            }
        }

        assertEquals(
            expected = "<div><div><p>Hello World!</p></div></div>",
            actual = root.outerHTML
        )
    }

    @Test
    fun canUpdateElementsCreatedInDomSideEffect() = runTest {
        var i: Int by mutableStateOf(0)
        val disposeCalls = mutableListOf<Int>()

        @Composable
        fun CustomDiv(value: Int) {
            Div {
                DomSideEffect(value) {
                    it.appendChild(
                        it.appendChild(document.createTextNode("Value = $value"))
                    )
                    onDispose {
                        disposeCalls.add(value)
                        it.clear()
                    }
                }
            }
        }

        composition { CustomDiv(i) }

        assertEquals(
            expected = "<div><div>Value = 0</div></div>",
            actual = root.outerHTML
        )

        i = 1

        waitForChanges()
        assertEquals(
            expected = 1,
            actual = disposeCalls.size,
            message = "Only one onDispose call expected"
        )
        assertEquals(
            expected = 0,
            actual = disposeCalls[0],
            message = "onDispose should be called with a previous value"
        )
        assertEquals(
            expected = "<div><div>Value = 1</div></div>",
            actual = root.outerHTML
        )
    }

    @Test
    fun onDisposeIsCalledWhenComposableRemovedFromComposition() = runTest {
        var showDiv: Boolean by mutableStateOf(true)
        var onDisposeCalledTimes = 0

        composition {
            if (showDiv) {
                Div {
                    DomSideEffect {
                        it.appendChild(document.createTextNode("Goedemorgen!"))
                        onDispose { onDisposeCalledTimes++ }
                    }
                }
            }
        }

        assertEquals(
            expected = "<div><div>Goedemorgen!</div></div>",
            actual = root.outerHTML
        )

        showDiv = false

        waitForChanges()
        assertEquals(1, onDisposeCalledTimes)
        assertEquals(expected = "<div></div>", actual = root.outerHTML)
    }

    @Test
    fun sideEffectsOrder() = runTest {
        val effectsList = mutableListOf<String>()

        var key = 1
        var recomposeScope: RecomposeScope? = null

        composition {
            recomposeScope = currentRecomposeScope

            Div {
                DomSideEffect(key) {
                    effectsList.add("DomSideEffect")
                }
                DisposableRefEffect(key) {
                    effectsList.add("DisposableRefEffect")
                    onDispose {
                        effectsList.add("DisposableRefEffectDisposed")
                    }
                }
            }
        }

        assertContentEquals(effectsList, listOf("DisposableRefEffect", "DomSideEffect"))

        key = 2
        recomposeScope?.invalidate()

        waitForRecompositionComplete()

        assertContentEquals(
            effectsList,
            listOf("DisposableRefEffect", "DomSideEffect", "DisposableRefEffectDisposed", "DisposableRefEffect", "DomSideEffect")
        )
    }

    @Test
    fun domSideEffectWithDisposableEffectTest() = runTest {
        val effectsList = mutableListOf<String>()

        var key = 1
        var recomposeScope: RecomposeScope? = null

        composition {
            recomposeScope = currentRecomposeScope

            Div {
                DisposableEffect(Unit) {
                    effectsList.add("DisposableEffectOneTime")
                    onDispose {  }
                }
                DomSideEffect(key) {
                    effectsList.add("DomSideEffect")
                }
                DisposableEffect(key) {
                    effectsList.add("DisposableEffect")
                    onDispose {
                        effectsList.add("DisposableEffectDisposed")
                    }
                }
            }
        }

        assertContentEquals(effectsList, listOf("DisposableEffectOneTime", "DisposableEffect", "DomSideEffect"))

        key = 2
        recomposeScope?.invalidate()

        waitForRecompositionComplete()

        assertContentEquals(
            effectsList,
            listOf("DisposableEffectOneTime", "DisposableEffect", "DomSideEffect", "DisposableEffectDisposed", "DisposableEffect", "DomSideEffect")
        )
    }

}
