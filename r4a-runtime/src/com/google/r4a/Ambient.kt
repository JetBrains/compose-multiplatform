package com.google.r4a

import android.view.ViewGroup
import kotlin.reflect.KProperty

class Ambient<T>(private val key: String, private val defaultFactory: (() -> T)? = null) {
    internal val defaultValue by lazy {
        val fn = defaultFactory
        if (fn != null) fn()
        else null as T
    }

    companion object {
        inline fun <reified T> of(
            key: String = T::class.java.simpleName,
            noinline defaultFactory: (() -> T)? = null
        ) = Ambient<T>(key, defaultFactory)
    }

    override fun hashCode() = key.hashCode()
    override fun equals(other: Any?) = this === other

    inner class Provider(var value: T, var children: () -> Unit) : Component() {
        internal val subscribers = HashSet<Slot>()

        override fun compose() {
            val cc = CompositionContext.current

            cc.start(0, this@Ambient)
            children()
            cc.end()
        }
    }

    inner class Consumer : Component() {
        lateinit var children: (T) -> Unit

        override fun compose() {
            val cc = CompositionContext.current
            val value = cc.getAmbient(this@Ambient)
            cc.start(0)
            children(value)
            cc.end()
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (thisRef !is Component) error("Cannot use Ambient property delegates on non-component instances")
        return CompositionContext.getAmbient(this, thisRef)
    }


    interface Reference {
        fun <T> getAmbient(key: Ambient<T>): T
        fun composeInto(container: ViewGroup, composable: () -> Unit)
    }

    class Portal(
        @Children
        var children: (ref: Reference) -> Unit
    ) : Component() {

        private val reference = object : Reference {
            override fun <T> getAmbient(key: Ambient<T>) = CompositionContext.getAmbient(key, this@Portal)
            override fun composeInto(container: ViewGroup, composable: () -> Unit) = R4a.composeInto(container, this, composable)
        }
        override fun compose() {
            children(reference)
        }
    }
}
