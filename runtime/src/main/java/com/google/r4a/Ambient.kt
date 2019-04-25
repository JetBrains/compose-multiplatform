package com.google.r4a

class Ambient<T>(private val key: String, private val defaultFactory: (() -> T)? = null) {
    @Suppress("UNCHECKED_CAST")
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
            with(currentComposerNonNull) {
                startProvider(this@Provider, value)
                children()
                endProvider()
            }
        }

        val ambient = this@Ambient
    }
}
