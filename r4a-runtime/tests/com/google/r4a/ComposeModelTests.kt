package com.google.r4a

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.frames.*
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

val PRESIDENT_NAME_1 = "George Washington"
val PRESIDENT_AGE_1 = 57
val PRESIDENT_NAME_16 = "Abraham Lincoln"
val PRESIDENT_AGE_16 = 52

class Person(name: String, age: Int) : Framed {
    var name
        get() = (_readable(_first, this) as CustomerRecord).name
        set(value) {
            (_writable(_first, this) as CustomerRecord).name = value
        }

    var age
        get() = (_readable(_first, this) as CustomerRecord).age
        set(value) {
            (_writable(_first, this) as CustomerRecord).age = value
        }

    private var _first: Record = CustomerRecord().apply {
        this.name = name
        this.age = age
        this.frameId = currentFrame().id
        _created(this@Person)
    }

    override val firstFrameRecord: Record get() = _first

    override fun prependFrameRecord(value: Record) {
        value.next = _first
        _first = value
    }

    class CustomerRecord : AbstractRecord() {
        @JvmField
        var name: String = ""
        @JvmField
        var age: Int = 0

        override fun assign(value: Record) {
            (value as? CustomerRecord)?.let {
                this.name = it.name
                this.age = it.age
            }
        }

        override fun create() = CustomerRecord()
    }
}

class State<T>(value: T) : Framed {
    @Suppress("UNCHECKED_CAST")
    var value: T
        get() = (_readable(myFirst, this) as StateRecord<T>).value
        set(value) {
            (_writable(myFirst, this) as StateRecord<T>).value = value
        }

    private var myFirst: Record

    init {
        myFirst = StateRecord(value)
    }

    override val firstFrameRecord: Record
        get() = myFirst

    override fun prependFrameRecord(value: Record) {
        value.next = myFirst
        myFirst = value
    }

    private class StateRecord<T>(myValue: T) : AbstractRecord() {
        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateRecord<T>).value
        }

        override fun create(): Record = StateRecord(value)

        var value: T = myValue
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ModelViewTests : TestCase() {

    @Before
    fun beforeTest() {
        val scheduler = RuntimeEnvironment.getMasterScheduler()
        scheduler.pause()
    }

    @Test
    fun testModelView_Simple(): Unit = isolated {
        val tvId = 67
        compose {
            emit(62, { context -> TextView(context).apply { text = "Hello world!"; id = tvId } }) { }
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }
    }

    @Test
    fun testModelView_Simple_Recompose(): Unit = isolated {
        val tvId = 71
        compose {
            emit(73, { context -> TextView(context).apply { text = "Hello world!"; id = tvId } }) { }
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }
    }

    @Test
    fun testModelView_PersonModel(): Unit = isolated {
        val tvIdName = 90
        val tvIdAge = 91
        val president = Person(PRESIDENT_NAME_1, PRESIDENT_AGE_1)
        compose {
            call(147, { true }) {
                Observe {
                    emit(93, { context -> TextView(context).apply { id = tvIdName } }) { set(president.name) { text = it } }
                    emit(94, { context -> TextView(context).apply { id = tvIdAge } }) { set(president.age) { text = it.toString() } }
                }
            }
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            president.name = PRESIDENT_NAME_16
            president.age = PRESIDENT_AGE_16
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test
    fun testModelView_RecomposeScopeCleanup(): Unit = isolated {
        val washington = Person(PRESIDENT_NAME_1, PRESIDENT_AGE_1)
        val lincoln = Person(PRESIDENT_NAME_16, PRESIDENT_AGE_16)
        val displayLincoln = State(true)

        fun ViewComposition.display(person: Person) {
            call(167, { true }) {
                Observe {
                    emit(93, { context -> TextView(context) }) { set(person.name) { text = it } }
                    emit(94, { context -> TextView(context) }) { set(person.age) { text = it.toString() } }
                }
            }
        }

        compose {
            call(185, { true }) {
                Observe {
                    display(washington)
                    if (displayLincoln.value)
                        display(lincoln)
                }
            }
        }.then {
            displayLincoln.value = false
        }.then {
            assertFalse(displayLincoln.value)
        }.then {
            assertFalse(displayLincoln.value)
        }
    }

    private class Root(val block: ViewComposition.() -> Unit) : Component() {
        override fun compose() {
            ViewComposition((CompositionContext.current as ComposerCompositionContext).composer).block()
        }
    }

    fun compose(block: ViewComposition.() -> Unit) = CompositionModelTest(block)

    class CompositionModelTest(val composable: ViewComposition.() -> Unit) {

        inner class ActiveTest(val activity: Activity) {
            private var firstCompose = true
            private fun compose() {
                val scheduler = RuntimeEnvironment.getMasterScheduler()
                scheduler.advanceToLastPostedRunnable()
                if (firstCompose) {
                    val cc = CompositionContext.current as ComposerCompositionContext
                    cc.startRoot()
                    val instance = Root(composable)
                    cc.setInstance(instance)
                    cc.startCompose(true)
                    instance()
                    cc.endCompose(true)
                    cc.endRoot()
                    firstCompose = false
                    cc.composer.applyChanges()
                }
            }

            fun then(block: (activity: Activity) -> Unit): ActiveTest {
                compose()
                block(activity)
                return this
            }
        }

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val controller = Robolectric.buildActivity(FrameTestActivity::class.java)
            val activity = controller.create().get()
            CompositionContext.current = CompositionContext.factory(activity, activity.root, Root(composable), null)
            return ActiveTest(activity).then(block)
        }
    }
}

private val Activity.root get() = findViewById(ComposerComposeTestCase.ROOT_ID) as ViewGroup

private class FrameTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ComposerComposeTestCase.ROOT_ID })
    }
}

