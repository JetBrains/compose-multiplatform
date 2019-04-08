package com.google.r4a

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import org.junit.runner.RunWith
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class EffectTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ComposerComposeTestCase.ROOT_ID })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class EffectsTests : TestCase() {

    @Test
    fun testMemoization1() {
        var inc = 0

        compose {
            +memo { ++inc }
        }.then { _ ->
            assertEquals(1, inc)
        }.then { _ ->
            assertEquals(1, inc)
        }
    }

    @Test
    fun testMemoization2() {
        var calculations = 0
        var compositions = 0
        var calculation = 0
        var key = 0

        compose {
            compositions++
            calculation = +memo(key) { 100 * ++calculations }
        }.then { _ ->
            assertEquals(1, calculations)
            assertEquals(100, calculation)
            assertEquals(1, compositions)
        }.then { _ ->
            assertEquals(1, calculations)
            assertEquals(100, calculation)
            assertEquals(2, compositions)
            key++
        }.then { _ ->
            assertEquals(2, calculations)
            assertEquals(200, calculation)
            assertEquals(3, compositions)
        }.then { _ ->
            assertEquals(2, calculations)
            assertEquals(200, calculation)
            assertEquals(4, compositions)
            key++
        }.then { _ ->
            assertEquals(3, calculations)
            assertEquals(300, calculation)
            assertEquals(5, compositions)
        }
    }

    @Test
    fun testState1() {
        val tv1Id = 100
        var inc = 0
        var local = State("invalid")

        compose {
            local = +state { "Hello world! ${inc++}" }
            composer.emit(
                168,
                { context -> TextView(context).apply { id = tv1Id } },
                { set(local.value) { text = it } }
            )
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
            assertEquals(local.value, helloText.text)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
            assertEquals(local.value, helloText.text)

            local.value = "New string"
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("New string", helloText.text)
            assertEquals(local.value, helloText.text)
        }
    }

    @Test
    fun testState2() {
        val tv1Id = 100
        val tv2Id = 200
        var local1 = State("invalid")
        var local2 = State("invalid")

        compose {
            local1 = +state { "First" }
            local2 = +state { "Second" }
            composer.emit(
                168,
                { context -> TextView(context).apply { id = tv1Id } },
                { set(local1.value) { text = it } }
            )
            composer.emit(
                169,
                { context -> TextView(context).apply { id = tv2Id } },
                { set(local2.value) { text = it } }
            )
        }.then { activity ->
            val tv1 = activity.findViewById(tv1Id) as TextView
            val tv2 = activity.findViewById(tv2Id) as TextView
            assertEquals("First", tv1.text)
            assertEquals("Second", tv2.text)
            assertEquals(local1.value, tv1.text)
            assertEquals(local2.value, tv2.text)
        }.then { activity ->
            val tv1 = activity.findViewById(tv1Id) as TextView
            val tv2 = activity.findViewById(tv2Id) as TextView
            assertEquals("First", tv1.text)
            assertEquals("Second", tv2.text)
            assertEquals(local1.value, tv1.text)
            assertEquals(local2.value, tv2.text)

            local1.value = "New First"
        }.then { activity ->
            val tv1 = activity.findViewById(tv1Id) as TextView
            val tv2 = activity.findViewById(tv2Id) as TextView
            assertEquals("New First", tv1.text)
            assertEquals("Second", tv2.text)
            assertEquals(local1.value, tv1.text)
            assertEquals(local2.value, tv2.text)
        }
    }

    @Test
    fun testDidCommit1() {
        var mount = true

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            +onCommit {
                log("onCommit")
                onDispose {
                    log("onDispose")
                }
            }
            log("Unmountable:end")
        }

        compose {
            with(composer) {
                log("compose:start")
                if (mount) {
                    call(
                        168,
                        { true },
                        { @Suppress("PLUGIN_ERROR") Unmountable() }
                    )
                }
                log("compose:end")
            }
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "onCommit"
                ),
                logHistory
            )
            mount = false
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "onCommit",
                    "compose:start",
                    "compose:end",
                    "onDispose"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testDidCommit2() {
        var mount = true

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            +onCommit {
                log("onCommit:a2")
                onDispose {
                    log("onDispose:a2")
                }
            }
            +onCommit {
                log("onCommit:b2")
                onDispose {
                    log("onDispose:b2")
                }
            }
        }

        compose {
            +onCommit {
                log("onCommit:a1")
                onDispose {
                    log("onDispose:a1")
                }
            }
            if (mount) {
                composer.call(
                    168,
                    { true },
                    { @Suppress("PLUGIN_ERROR") Unmountable() }
                )
            }
            +onCommit {
                log("onCommit:b1")
                onDispose {
                    log("onDispose:b1")
                }
            }
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onCommit:a1",
                    "onCommit:a2",
                    "onCommit:b2",
                    "onCommit:b1"
                ),
                logHistory
            )
            mount = false
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onCommit:a1",
                    "onCommit:a2",
                    "onCommit:b2",
                    "onCommit:b1",
                    "recompose",
                    "onDispose:b2",
                    "onDispose:a2",
                    "onDispose:b1",
                    "onDispose:a1",
                    "onCommit:a1",
                    "onCommit:b1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testOnCommit3() {
        var x = 0

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            +onCommit {
                val y = x++
                log("commit:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }.then { _ ->
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "commit:0",
                    "recompose",
                    "dispose:0",
                    "commit:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testOnCommit31() {
        var a = 0
        var b = 0

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            +onCommit {
                val y = a++
                log("commit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }
            +onCommit {
                val y = b++
                log("commit b:$y")
                onDispose {
                    log("dispose b:$y")
                }
            }
        }.then { _ ->
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "commit a:0",
                    "commit b:0",
                    "recompose",
                    "dispose b:0",
                    "dispose a:0",
                    "commit a:1",
                    "commit b:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testOnCommit4() {
        var x = 0
        var key = 123

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            +onCommit(key) {
                val y = x++
                log("commit:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }.then { _ ->
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "commit:0",
                    "recompose"
                ),
                logHistory
            )
            log("recompose (key -> 345)")
            key = 345
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "commit:0",
                    "recompose",
                    "recompose (key -> 345)",
                    "dispose:0",
                    "commit:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testOnCommit5() {
        var a = 0
        var b = 0
        var c = 0

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Sub() {
            +onCommit {
                val y = c++
                log("commit c:$y")
                onDispose {
                    log("dispose c:$y")
                }
            }
        }

        compose {
            +onCommit {
                val y = a++
                log("commit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }

            +onCommit {
                val y = b++
                log("commit b:$y")
                onDispose {
                    log("dispose b:$y")
                }
            }

            composer.call(
                1234,
                { true },
                { @Suppress("PLUGIN_ERROR") Sub() }
            )
        }.then { _ ->
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "commit a:0",
                    "commit b:0",
                    "commit c:0",
                    "recompose",
                    "dispose c:0",
                    "dispose b:0",
                    "dispose a:0",
                    "commit a:1",
                    "commit b:1",
                    "commit c:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testAmbient1() {
        val tv1Id = 100

        val Foo = Ambient.of<String>()
        var current = "Hello World"

        @Composable
        fun Bar() {
            composer.call(
                21323,
                { true },
                {
                    @Suppress("PLUGIN_ERROR")
                    Observe {
                        val foo = +ambient(Foo)
                        composer.emit(
                            168,
                            { context -> TextView(context).apply { id = tv1Id } },
                            { set(foo) { text = it } }
                        )
                    }
                }
            )
        }

        compose {
            with(composer) {
                provideAmbient(Foo, current) {
                    call(
                        123,
                        { false },
                        { @Suppress("PLUGIN_ERROR") Bar() }
                    )
                }
            }
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals(current, helloText.text)
            current = "abcd"
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals(current, helloText.text)
        }
    }

    @Test
    fun testAmbient2() {

        val MyAmbient = Ambient<Double>("Hello") { throw Exception("not set") }

        var requestRecompose: (() -> Unit)? = null
        var buttonCreated = false

        fun SimpleComposable2() {
            with(composer) {
                consumeAmbient(MyAmbient) { value ->
                    emit(534, { context -> TextView(context) }, {
                        set("$value") { text = it }
                    })
                }
            }
        }

        fun SimpleComposable() {
            composer.call(531, {
                Recompose().apply {
                    body = { recompose ->
                        requestRecompose = recompose
                        composer.provideAmbient(MyAmbient, Math.random()) {
                            composer.call(523, { false }) { SimpleComposable2() }
                            composer.emitView(525, { context -> Button(context).also {
                                buttonCreated = true
                            } })
                        }
                    }
                }
            }, { true }) { f -> @Suppress("PLUGIN_ERROR") f.invoke() }
        }

        fun Root() {
            with(composer) {
                call(547, { false }) {
                    SimpleComposable()
                }
            }
        }

        compose {
            with(composer) {
                call(556, { false }) {
                    Root()
                }
            }
        }.then {
            assertTrue("Expected button to be created", buttonCreated)
            buttonCreated = false
            requestRecompose?.invoke()
        }.then {
            assertFalse("Expected button to not be recreated", buttonCreated)
            requestRecompose?.invoke()
        }
    }

    @Test
    fun testUpdatedComposition() {
        val tv1Id = 100
        var inc = 0

        compose {
            val local = +state { "Hello world! ${inc++}" }
            composer.emit(
                168,
                { context -> TextView(context).apply { id = tv1Id } },
                { set(local.value) { text = it } }
            )
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
        }
    }

    class CompositionTest(val composable: () -> Unit) {
        inner class ActiveTest(
            val activity: Activity,
            val cc: CompositionContext,
            val component: Component
        ) {

            fun then(block: (activity: Activity) -> Unit): ActiveTest {
                cc.recomposeSync(component)
                block(activity)
                return this
            }
        }

        private class Root(var composable: () -> Unit) : Component() {
            override fun compose() = composable()
        }

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val controller = Robolectric.buildActivity(EffectTestActivity::class.java)
            val activity = controller.create().get()
            val root = activity.root
            val component = Root(composable)
            val cc = CompositionContext.create(root.context, root, component, null)
            cc.context = activity
            return ActiveTest(activity, cc, component).then(block)
        }
    }

    fun compose(composable: () -> Unit) = CompositionTest(composable)
}

private val Activity.root get() = findViewById(ComposerComposeTestCase.ROOT_ID) as ViewGroup

fun <T> assertArrayEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    transform: (T) -> String = { "$it" }
) {
    assertEquals(
        expected.joinToString("\n", transform = transform),
        actual.joinToString("\n", transform = transform)
    )
}