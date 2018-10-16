package com.google.r4a

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


private class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ComposeTestCase.ROOT_ID })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class NewCodeGenTests : TestCase() {

    @Test
    fun testStaticComposition() {
        val tv1Id = 100
        val tv2Id = 200

        compose {
            emit(168, { TextView(it).apply { text = "Hello world!"; id = tv1Id } }) { }

            emit(170, { LinearLayout(it).apply { orientation = LinearLayout.HORIZONTAL } }, { }) {
                emit(171, { TextView(it).apply { text = "Yellow world"; id = tv2Id } }) { }
            }
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world!", helloText.text)
            val yellowText = activity.findViewById(tv2Id) as TextView
            assertEquals("Yellow world", yellowText.text)
        }
    }

    @Test
    fun testUpdatedComposition() {
        val tv1Id = 100
        val tv2Id = 200

        var text1 = "Hello world!"
        var text2 = "Yellow world"

        compose {
            emit(168, { TextView(it).apply { id = tv1Id } }) {
                set(text1) { text = it }
            }
            emit(170, { LinearLayout(it).apply { orientation = LinearLayout.HORIZONTAL } }, { }) {
                emit(171, { TextView(it).apply { id = tv2Id } }) {
                    set(text2) { text = it }
                }
            }
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals(text1, helloText.text)
            val yellowText = activity.findViewById(tv2Id) as TextView
            assertEquals(text2, yellowText.text)

            // Modify the composed state
            text1 += " (changed)"
            text2 += " (changed)"
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals(text1, helloText.text)
            val yellowText = activity.findViewById(tv2Id) as TextView
            assertEquals(text2, yellowText.text)
        }
    }

    @Test
    fun testSingleView() {
        val tvId = 237
        var text = "Hello world"

        compose {
            // <TextView text id=tvId />
            emit(242, { TextView(it).apply { id = tvId } }) {
                set(text) { this.text = it }
            }
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("Hello world", tv.text)

            text = "Salutations!"
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("Salutations!", tv.text)
        }
    }

    @Test
    fun testViewGroup() {
        val tvId = 258
        val llId = 260
        var text = "Hello world"
        var orientation = LinearLayout.HORIZONTAL

        compose {
            // <LinearLayout>
            //  <TextView text />
            // </LinearLayout
            emit(264, { LinearLayout(it).apply { id = llId } }, {
                set(orientation) { this.orientation = it }
            }) {
                emit(265, { TextView(it).apply { id = tvId } }) {
                    set(text) { this.text = it }
                }
            }
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("Hello world", tv.text)

            text = "Salutations!"
            orientation = LinearLayout.VERTICAL
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("Salutations!", tv.text)

            val ll = activity.findViewById(llId) as LinearLayout
            assertEquals(LinearLayout.VERTICAL, ll.orientation)
        }
    }

    @Test
    fun testComposableFunctionInvocationOneParameter() {
        data class Phone(val area: String, val prefix: String, val number: String)

        var phone = Phone("123", "456", "7890")
        var phoneCalled = 0
        compose {

            // Composition function
            //  @Composable
            //  fun PhoneView(phone: Phone) {
            //    phoneCalled++
            //   <TextView text="..." />
            //  }
            fun PhoneView(phone: Phone) {
                phoneCalled++
                emit(225, ::TextView) {
                    set("${if (phone.area.isBlank()) "" else "(${phone.area}) "}${phone.prefix}-${phone.number}") { text = it }
                }
            }

            // <PhoneView phone />
            call(453, { changed(phone) }) {
                PhoneView(phone)
            }
        }.then { activity ->
            assertEquals(1, phoneCalled)
        }.then { activity ->
            assertEquals(1, phoneCalled)

            phone = Phone("124", "456", "7890")
        }.then { activity ->
            assertEquals(2, phoneCalled)
        }
    }

    @Test
    fun testComposableFunctionInvocationTwoParameters() {
        val tvId = 279
        var left = 0
        var right = 1
        var addCalled = 0
        compose {

            // Composition function
            //  @Composable
            //  fun AddView(left: Int, right: Int) {
            //    addCalled++
            //   <TextView text="$left + $right = ${left + right}" />
            //  }
            fun AddView(left: Int, right: Int) {
                addCalled++
                emit(292, { TextView(it).apply { id = tvId } }) {
                    set("$left + $right = ${left + right}") { text = it }
                }
            }

            // <AddView left right />
            call(491, { changed(left) + changed(right) }) {
                AddView(left, right)
            }
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            left = 1
        }.then { activity ->
            assertEquals(2, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            assertEquals(2, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            right = 41
        }.then { activity ->
            assertEquals(3, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }
    }

    @Test
    fun testStatelessComposableClassInvocationProperties() {
        val tvId = 338
        var addCalled = 0

        var left = 0
        var right = 0
        compose {
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a local class capturing a variable so this make the capture explicit
            class AddView(val composition: ViewComposition) {
                var left = 0
                var right = 0

                operator fun invoke() {
                    with(composition) {
                        // <TextView "$left + $right = ${left + right}" />
                        emit(350, { TextView(it).apply { id = tvId } }) {
                            addCalled++
                            set("${this@AddView.left} + ${this@AddView.right} = ${this@AddView.left + this@AddView.right}") { text = it }
                        }
                    }
                }
            }

            // <AddView left right />
            call(358,
                 { AddView(this@compose) },
                 { f -> set(left) { f.left = it } + set(right) { f.right = it } }) { f ->
                f()
            }
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            left = 1
        }.then { activity ->
            assertEquals(2, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            assertEquals(2, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            right = 41
        }.then { activity ->
            assertEquals(3, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }
    }


    @Test
    fun testStatelessComposableClassInvocationParameters() {
        val tvId = 338
        var addCalled = 0

        var left = 0
        var right = 0

        compose {
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a local class capturing a variable so this make the capture explicit
            class AddView(val composition: ViewComposition) {

                operator fun invoke(left: Int, right: Int) {
                    with(composition) {
                        // <TextView "$left + $right = ${left + right}" />
                        emit(350, { TextView(it).apply { id = tvId } }) {
                            addCalled++
                            set("$left + $right = ${left + right}") { text = it }
                        }
                    }
                }
            }

            // <AddView left right />
            call(612, { changed(left) + changed(right) }) {
                AddView(this@compose)(left, right)
            }
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            left = 1
        }.then { activity ->
            assertEquals(2, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            assertEquals(2, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            right = 41
        }.then { activity ->
            assertEquals(3, addCalled)

            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }
    }

    @Test
    fun testStatefulComposableClassInvocation() {
        val tvId = 470
        val tvPrivateValue = 471
        var addCalled = 0

        var left = 0
        var right = 0

        lateinit var updatePrivate: (value: String) -> Unit

        compose {
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a local class capturing a variable so this make the capture explicit
            class AddView(val composition: ViewComposition) {

                var left = 0
                var right = 0

                private var privateValue = "Unmodified"

                operator fun invoke() {
                    with(composition) {
                        addCalled++

                        // <TextView "$left + $right = ${left + right}" />
                        emit(491, { TextView(it).apply { id = tvId } }) {
                            set("${this@AddView.left} + ${this@AddView.right} = ${this@AddView.left + this@AddView.right}") { text = it }
                        }

                        emit(496, { TextView(it).apply { id = tvPrivateValue } }) {
                            set(privateValue) { text = it }
                        }

                        updatePrivate = {
                            privateValue = it
                            // TODO: This actually requires recompose(). Update when that is available
                            // recompose()
                        }
                    }
                }
            }

            // <AddView left right />
            // TODO: remove the + true when recompose() is available.
            call(690,
                 { AddView(this@compose) },
                 { f -> set(left) { f.left = it } + set(right) { f.right = it } + true }) { f ->
                f()
            }
        }.then { activity ->
            assertEquals(1, addCalled)
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            left = 1
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            right = 41
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            val privateTv = activity.findViewById(tvPrivateValue) as TextView
            assertEquals("Unmodified", privateTv.text)

            updatePrivate("Modified value")
        }.then { activity ->
            val privateTv = activity.findViewById(tvPrivateValue) as TextView
            assertEquals("Modified value", privateTv.text)
        }
    }


    @Test
    fun testStatefulComposableClassWithCtorParametersInvocation() {
        val tvId = 604
        val tvOffsetId = 605

        var offset = 0
        var left = 0
        var right = 0
        compose {
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a local class capturing a variable so this make the capture explicit
            class OffsetAddView(
                val composition: ViewComposition,
                var offset: Int
            ) {

                var left = 0
                var right = 0

                operator fun invoke() {
                    with(composition) {

                        // <TextView "$left + $right = ${left + right}" />
                        emit(619, { TextView(it).apply { id = tvId } }) {
                            set("${this@OffsetAddView.left} + ${this@OffsetAddView.right} = ${this@OffsetAddView.left + this@OffsetAddView.right}") {
                                text = it
                            }
                        }

                        // <TextView text="$offset" />
                        emit(623, { TextView(it).apply { id = tvOffsetId } }) {
                            set("$offset") { this.text = it }
                        }
                    }
                }
            }

            // <OffsetAddView offset left right />
            call(768,
                 { OffsetAddView(this@compose, offset) },
                 { f -> update(offset) { f.offset = it } + set(left) { f.left = it } + set(right) { f.right = it } }
            ) { f ->
                f()
            }
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            val offsetTv = activity.findViewById(tvOffsetId) as TextView
            assertEquals("$offset", offsetTv.text)

            offset = 30
        }.then { activity ->
            val offsetTv = activity.findViewById(tvOffsetId) as TextView
            assertEquals("$offset", offsetTv.text)

            left = 20
            right = 21
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }
    }

    @Test
    fun testStatefulComposableClassWithPivotalProperty() {
        val tvId = 604
        val tvOffsetId = 605

        var offset = 0
        var left = 0
        var right = 0
        compose {
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a local class capturing a variable so this make the capture explicit
            class OffsetAddView(
                val composition: ViewComposition,
                val offset: Int
            ) {

                var left = 0
                var right = 0

                operator fun invoke() {
                    with(composition) {

                        // <TextView "$left + $right = ${left + right}" />
                        emit(709, { TextView(it).apply { id = tvId } }) {
                            set("${this@OffsetAddView.left} + ${this@OffsetAddView.right} = ${this@OffsetAddView.left + this@OffsetAddView.right}") {
                                text = it
                            }
                        }

                        // <TextView text="$offset" />
                        emit(714, { TextView(it).apply { id = tvOffsetId } }) {
                            set("${this@OffsetAddView.offset}") { this.text = it }
                        }
                    }
                }
            }

            // <OffsetAddView offset left right />
            call(joinKey(831, offset),
                 { OffsetAddView(this@compose, offset) },
                 { f -> set(left) { f.left = it } + set(right) { f.right = it } }) { f ->
                f()
            }
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)

            val offsetTv = activity.findViewById(tvOffsetId) as TextView
            assertEquals("$offset", offsetTv.text)

            offset = 30
        }.then { activity ->
            val offsetTv = activity.findViewById(tvOffsetId) as TextView
            assertEquals("$offset", offsetTv.text)

            left = 20
            right = 21
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("$left + $right = ${left + right}", tv.text)
        }
    }

    @Test
    fun testViewClassWithCtorParametersInvocation() {
        val tvId = 749

        class MyTextView(context: Context) : TextView(context) {
            constructor(context: Context, someText: String) : this(context) {
                text = someText
            }
        }

        var hello = "Hello world!"
        compose {
            // <MyTextView someText=hello />
            emit(joinKey(760, hello), { MyTextView(it, hello).apply { id = tvId } }) { }
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)

            hello = "Salutations!"
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("Salutations!", tv.text)
        }
    }

    @Test
    fun testViewClassWithMutableCtorParameter() {
        val tvId = 749

        class MyTextView(context: Context, var someValue: String) : TextView(context)

        var hello = "Hello world!"
        var value = "Unmodified"
        compose {
            // <MyTextView someText=hello />
            emit(760, { MyTextView(it, value).apply { id = tvId } }) {
                update(value) { someValue = it }
                set(hello) { this.text = it }
            }
        }.then { activity ->
            val tv = activity.findViewById(tvId) as MyTextView
            assertEquals("Hello world!", tv.text)
            assertEquals("Unmodified", tv.someValue)

            hello = "Salutations!"
            value = "Modified"
        }.then { activity ->
            val tv = activity.findViewById(tvId) as MyTextView
            assertEquals("Salutations!", tv.text)
            assertEquals("Modified", tv.someValue)
        }
    }

    fun compose(block: ViewComposition.() -> Unit) = CompositionTest(block)

    class CompositionTest(val composable: ViewComposition.() -> Unit) {

        inner class ActiveTest(val composition: ViewComposition, val activity: Activity) {
            private fun compose() {
                composition.composer.slots.reset()
                composition.composer.slots.beginReading()
                composition.composer.startGroup(0)
                composition.composable()
                composition.composer.endGroup()
                composition.composer.slots.endReading()
                composition.composer.finalizeCompose()
                composition.composer.applyChanges()
            }

            fun then(block: (activity: Activity) -> Unit): ActiveTest {
                compose()
                block(activity)
                return this
            }
        }

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val controller = Robolectric.buildActivity(TestActivity::class.java)
            val activity = controller.create().get()
            val composition = ViewComposition(ViewComposer(activity.root, activity))
            return ActiveTest(composition, activity).then(block)
        }
    }
}

private val Activity.root get() = findViewById(ComposeTestCase.ROOT_ID) as ViewGroup
