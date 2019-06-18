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

package androidx.compose

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            id =
                ComposerComposeTestCase.ROOT_ID
        })
    }
}

@RunWith(ComposeRobolectricTestRunner::class)
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
            emit(168, { context ->
                TextView(context).apply {
                    text = "Hello world!"; id = tv1Id
                }
            }) { }

            emit(170, { context ->
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
            }, { }) {
                emit(171, { context ->
                    TextView(context).apply {
                        text = "Yellow world"; id = tv2Id
                    }
                }) { }
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
            emit(168, { context -> TextView(context).apply { id = tv1Id } }) {
                set(text1) { text = it }
            }
            emit(170, { context ->
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
            }, { }) {
                emit(171, { context -> TextView(context).apply { id = tv2Id } }) {
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
    fun testDisposeComposition() {
        class DisposeTestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                val root = FrameLayout(this)
                val log = mutableListOf<String>()
                val composable = @Composable {
                    +onPreCommit {
                        log.add("onPreCommit")
                        onDispose {
                            log.add("onPreCommitDispose")
                        }
                    }
                    +onActive {
                        log.add("onActive")
                        onDispose {
                            log.add("onActiveDispose")
                        }
                    }
                }

                val scheduler = RuntimeEnvironment.getMasterScheduler()

                log.clear()
                Compose.composeInto(container = root, composable = composable)
                scheduler.advanceToLastPostedRunnable()
                assertEquals("onPreCommit, onActive", log.joinToString())

                log.clear()
                Compose.composeInto(container = root, composable = composable)
                scheduler.advanceToLastPostedRunnable()
                assertEquals("onPreCommitDispose, onPreCommit", log.joinToString())

                log.clear()
                Compose.disposeComposition(container = root)
                scheduler.advanceToLastPostedRunnable()
                assertEquals("onActiveDispose, onPreCommitDispose", log.joinToString())

                log.clear()
                Compose.composeInto(container = root, composable = composable)
                scheduler.advanceToLastPostedRunnable()
                assertEquals("onPreCommit, onActive", log.joinToString())
            }
        }

        val controller = Robolectric.buildActivity(DisposeTestActivity::class.java)
        controller.create()
    }

    @Test
    fun testSingleView() {
        val tvId = 237
        var text = "Hello world"

        compose {
            // <TextView text id=tvId />
            emit(242, { context -> TextView(context).apply { id = tvId } }) {
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
            emit(264, { context: Context -> LinearLayout(context).apply { id = llId } }, {
                set(orientation) { this.orientation = it }
            }) {
                emit(265, { context -> TextView(context).apply { id = tvId } }) {
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
                emit(225, { context -> TextView(context) }) {
                    set(
                        "${if (phone.area.isBlank()) ""
                        else "(${phone.area}) "}${phone.prefix}-${phone.number}"
                    ) { text = it }
                }
            }

            // <PhoneView phone />
            call(453, { changed(phone) }) {
                PhoneView(phone)
            }
        }.then { _ ->
            assertEquals(1, phoneCalled)
        }.then { _ ->
            assertEquals(2, phoneCalled)

            phone = Phone("124", "456", "7890")
        }.then { _ ->
            assertEquals(3, phoneCalled)
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
                emit(292, { context -> TextView(context).apply { id = tvId } }) {
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
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a
            // local class capturing a variable so this make the capture explicit
            class AddView(val composition: ViewComposition) {
                var left = 0
                var right = 0

                operator fun invoke() {
                    with(composition) {
                        // <TextView "$left + $right = ${left + right}" />
                        emit(350, { context -> TextView(context).apply { id = tvId } }) {
                            addCalled++
                            set(
                                "${this@AddView.left} + ${this@AddView.right} = ${
                                this@AddView.left + this@AddView.right}"
                            ) { text = it }
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
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a
            // local class capturing a variable so this make the capture explicit
            class AddView(val composition: ViewComposition) {

                operator fun invoke(left: Int, right: Int) {
                    with(composition) {
                        // <TextView "$left + $right = ${left + right}" />
                        emit(350, { context -> TextView(context).apply { id = tvId } }) {
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
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a
            // local class capturing a variable so this make the capture explicit
            class AddView(val composition: ViewComposition) {

                var left = 0
                var right = 0

                private var privateValue = "Unmodified"

                operator fun invoke() {
                    with(composition) {
                        addCalled++

                        // <TextView "$left + $right = ${left + right}" />
                        emit(491, { context -> TextView(context).apply { id = tvId } }) {
                            set(
                                "${this@AddView.left} + ${this@AddView.right} = ${
                                this@AddView.left + this@AddView.right}"
                            ) { text = it }
                        }

                        emit(496, { context ->
                            TextView(context).apply {
                                id = tvPrivateValue
                            }
                        }) {
                            set(privateValue) { text = it }
                        }

                        updatePrivate = {
                            privateValue = it
                            // TODO: This actually requires recompose(). Update when that is
                            // available recompose()
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
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a
            // local class capturing a variable so this make the capture explicit
            class OffsetAddView(
                val composition: ViewComposition,
                var offset: Int
            ) {

                var left = 0
                var right = 0

                operator fun invoke() {
                    with(composition) {

                        // <TextView "$left + $right = ${left + right}" />
                        emit(619, { context -> TextView(context).apply { id = tvId } }) {
                            set(
                                "${this@OffsetAddView.left} + ${this@OffsetAddView.right} = ${
                                this@OffsetAddView.left + this@OffsetAddView.right}"
                            ) {
                                text = it
                            }
                        }

                        // <TextView text="$offset" />
                        emit(623, { context -> TextView(context).apply { id = tvOffsetId } }) {
                            set("$offset") { this.text = it }
                        }
                    }
                }
            }

            // <OffsetAddView offset left right />
            call(768,
                { OffsetAddView(this@compose, offset) },
                { f ->
                    update(offset) { f.offset = it } + set(left) { f.left = it } + set(right) {
                        f.right = it
                    }
                }
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
            // TODO: The composition field is a work-around for an IR bug. The IR doesn't support a
            // local class capturing a variable so this make the capture explicit
            class OffsetAddView(
                val composition: ViewComposition,
                val offset: Int
            ) {

                var left = 0
                var right = 0

                operator fun invoke() {
                    with(composition) {

                        // <TextView "$left + $right = ${left + right}" />
                        emit(709, { context -> TextView(context).apply { id = tvId } }) {
                            set(
                                "${this@OffsetAddView.left} + ${this@OffsetAddView.right} = ${
                                this@OffsetAddView.left + this@OffsetAddView.right}"
                            ) {
                                text = it
                            }
                        }

                        // <TextView text="$offset" />
                        emit(714, { context ->
                            TextView(context).apply {
                                id = tvOffsetId
                            }
                        }) {
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
    fun testMoveComponents() {
        val data = mutableListOf(1, 2, 3, 4, 5)
        compose {
            for (item in data) {
                emit(joinKey(560, item), { context ->
                    TextView(context).apply { text = "$item View" }
                }) { }
            }
        }.then {
            data.add(data.removeAt(0))
        }.then { activity ->
            val root = activity.root
            for (index in 0 until data.size) {
                val textView = root.getChildAt(index) as TextView
                assertEquals("${data[index]} View", textView.text)
            }
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
            emit(joinKey(760, hello), { context ->
                MyTextView(context, hello).apply { id = tvId }
            }) { }
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
            emit(760, { context -> MyTextView(context, value).apply { id = tvId } }) {
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

    @Test
    fun testEmittingAnEmittable() {

        class MyEmittable : MockEmittable() {
            var message: String = ""
        }

        compose {
            adaptable {
                emit(615, { context -> LinearLayout(context) }, {}) {
                    emit(616, { -> MyEmittable() }, { set("Message") { message = it } }) {
                        emit(617, { context -> TextView(context) }, {
                            set("SomeValue") { text = it }
                        })
                        emit(620, { -> MyEmittable() }, { set("Message2") { message = it } })
                    }
                }
            }
        }.then { activity ->
            val root = activity.root.getChildAt(0) as LinearLayout
            val firstChild = root.getChildAt(0) as ViewEmitWrapper
            val emitted = firstChild.emittable as MockEmittable
            val firstEmitChild = emitted.children[0] as EmitViewWrapper
            firstEmitChild.view as TextView
            val secondEmit = emitted.children[1] as MyEmittable
            assertEquals(0, secondEmit.children.size)
        }
    }

    @Test
    fun testCGEmittingAnEmittable() {

        class MyEmittable : MockEmittable() {
            var message: String = ""
        }

        composeCG {
            adaptable {
                val cc = composer
                cc.emitViewGroup(615, { context -> LinearLayout(context) }, {}) {
                    cc.emitEmittable(616, { -> MyEmittable() }, {
                        set("Message") { message = it }
                    }) {
                        cc.emit(617, { context -> TextView(context) }, {
                            set("SomeValue") { text = it }
                        })
                        cc.emitEmittable(620, { -> MyEmittable() }, {
                            set("Message2") { message = it }
                        })
                    }
                }
            }
        }.then { activity ->
            val root = activity.root.getChildAt(0) as LinearLayout
            val firstChild = root.getChildAt(0) as ViewEmitWrapper
            val emitted = firstChild.emittable as MockEmittable
            val firstEmitChild = emitted.children[0] as EmitViewWrapper
            firstEmitChild.view as TextView
            val secondEmit = emitted.children[1] as MyEmittable
            assertEquals(0, secondEmit.children.size)
        }.then { activity ->
            val root = activity.root.getChildAt(0) as LinearLayout
            val firstChild = root.getChildAt(0) as ViewEmitWrapper
            val emitted = firstChild.emittable as MockEmittable
            val firstEmitChild = emitted.children[0] as EmitViewWrapper
            firstEmitChild.view as TextView
            val secondEmit = emitted.children[1] as MyEmittable
            assertEquals(0, secondEmit.children.size)
        }
    }

    @Test
    fun testCGEmittableAsRoot() {
        class MyEmittable : MockEmittable() {
            var message: String = ""
        }

        val root = MyEmittable()

        var first = "Hi"
        var second = "there"
        composeCG { activity ->
            adaptable {
                Compose.composeInto(root, activity) {
                    val cc = composer
                    cc.emitEmittable(686, { MyEmittable() }, { set(first) { message = it } }) {
                        cc.emitEmittable(687, { MyEmittable() }, {
                            set(second) { message = it }
                        })
                    }
                }
            }
        }.then {
            assertEquals(first, (root.children.first() as MyEmittable).message)
            assertEquals(
                second,
                ((root.children.first() as MyEmittable).children.first() as MyEmittable).message
            )

            first = "hello"
            second = "dolly"
        }.then {
            assertEquals(first, (root.children.first() as MyEmittable).message)
            assertEquals(
                second,
                ((root.children.first() as MyEmittable).children.first() as MyEmittable).message
            )
        }
    }

    open class MockEmittable : Emittable {
        val children = mutableListOf<Emittable>()
        override fun emitInsertAt(index: Int, instance: Emittable) {
            children.add(index, instance)
        }

        override fun emitRemoveAt(index: Int, count: Int) {
            children.subList(index, count).clear()
        }

        override fun emitMove(from: Int, to: Int, count: Int) {
            val range = children.subList(from, count)
            val moved = range.map { it }
            range.clear()
            children.addAll(if (to > from) to - count else to, moved)
        }
    }

    class ViewEmitWrapper(context: Context) : View(context) {
        var emittable: Emittable? = null
    }

    class EmitViewWrapper : MockEmittable() {
        var view: View? = null
    }

    fun ViewComposition.adaptable(block: ViewComposition.() -> Unit) {
        composer.adapters?.register { parent, child ->
            when (parent) {
                is ViewGroup -> when (child) {
                    is View -> child
                    is Emittable -> ViewEmitWrapper(composer.context).apply { emittable = child }
                    else -> null
                }
                is Emittable -> when (child) {
                    is View -> EmitViewWrapper().apply { view = child }
                    is Emittable -> child
                    else -> null
                }
                else -> null
            }
        }
        block()
    }

    fun compose(block: ViewComposition.() -> Unit) =
        CompositionTest(block)

    class CompositionTest(val composable: ViewComposition.() -> Unit) {

        inner class ActiveTest(val composition: ViewComposition, val activity: Activity) {
            private fun compose() {
                composition.composer.startRoot()
                composition.composable()
                composition.composer.endRoot()
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
            val composition = ViewComposition(
                ViewComposer(activity.root, activity, object : Recomposer() {
                    override fun scheduleChangesDispatch() {}

                    override fun hasPendingChanges(): Boolean = false
                })
            )
            return ActiveTest(composition, activity).then(block)
        }
    }

    class TestContext(val cc: CompositionContext) {
        fun adaptable(block: TestContext.() -> Unit) {
            composer.registerAdapter { parent, child ->
                when (parent) {
                    is ViewGroup -> when (child) {
                        is View -> child
                        is Emittable -> ViewEmitWrapper(parent.context).apply { emittable = child }
                        else -> null
                    }
                    is Emittable -> when (child) {
                        is View -> EmitViewWrapper().apply { view = child }
                        is Emittable -> child
                        else -> null
                    }
                    else -> null
                }
            }
            block()
        }
    }

    fun composeCG(block: TestContext.(activity: Activity) -> Unit) =
        CompositionCodeGenTest(block)

    private class Root : Component() {
        override fun compose() {}
    }

    class CompositionCodeGenTest(val composable: TestContext.(activity: Activity) -> Unit) {
        inner class ActiveTest(
            val activity: Activity,
            val context: TestContext,
            val component: Component
        ) {

            fun then(block: TestContext.(activity: Activity) -> Unit): ActiveTest {
                val composer = context.cc.composer
                composer.runWithCurrent {
                    composer.startRoot()
                    context.composable(activity)
                    composer.endRoot()
                    composer.applyChanges()
                    context.block(activity)
                }
                return this
            }
        }

        fun then(block: TestContext.(activity: Activity) -> Unit): ActiveTest {
            val controller = Robolectric.buildActivity(TestActivity::class.java)
            val activity = controller.create().get()
            val root = activity.root
            val component = Root()
            val cc = Compose.createCompositionContext(root.context, root, component, null)
            return ActiveTest(activity, TestContext(cc), component).then(block)
        }
    }
}

private val Activity.root get() = findViewById(ComposerComposeTestCase.ROOT_ID) as ViewGroup
