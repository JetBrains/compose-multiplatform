package com.google.r4a

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
        var children: @Composable() () -> Unit
    ) : Component() {

        @Suppress("PLUGIN_ERROR")
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
        var children: @Composable() (T) -> Unit
    ) : Component() {

        @Suppress("PLUGIN_ERROR")
        override fun compose() {
            with(composer.composer) {
                children(consume(ambient))
            }
        }

        val ambient = this@Ambient
    }

    interface Reference {
        fun <T> getAmbient(key: Ambient<T>): T
        fun invalidate()
        fun <T> invalidateConsumers(key: Ambient<T>)
        fun <N> registerComposer(composer: Composer<N>)
    }

    class Portal(
        @Children
        var children: (ref: Reference) -> Unit
    ) : Component() {

        override fun compose() {
            children(composer.composer.buildReference())
        }
    }
}
