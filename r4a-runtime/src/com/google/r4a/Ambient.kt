package com.google.r4a

import android.view.ViewGroup

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

    inner class Provider(
        var value: T,
        @Children
        var children: @Composable() () -> Unit) : Component() {

        override fun compose() {
            with(composer.composer) {
                startProvider(this@Provider, value)
                children()
                endProvider()
            }
        }

        val ambient = this@Ambient
    }

    inner class Consumer(
        @Children
        var children: @Composable() (T) -> Unit) : Component() {

        override fun compose() {
            with(composer.composer) {
                children(consume(ambient))
            }
        }

        val ambient = this@Ambient
    }

    interface Reference {
        fun <T> getAmbient(key: Ambient<T>): T
        fun composeInto(container: ViewGroup, composable: @Composable() () -> Unit)
    }

    class Portal(
        @Children
        var children: (ref: Reference) -> Unit) : Component() {

        override fun compose() {
            children(composer.composer.buildReference())
        }
    }
}
