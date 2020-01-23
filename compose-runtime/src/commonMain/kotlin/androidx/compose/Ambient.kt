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

/**
 * Compose passes data through the composition tree explicitly through means of parameters to
 * composable functions. This is often times the simplest and best way to have data flow through
 * the tree.
 *
 * Sometimes this model can be cumbersome or break down for data that is needed by lots of
 * components, or when components need to pass data between one another but keep that implementation
 * detail private. For these cases, [Ambient]s can be used as an implicit way to have data flow
 * through a composition.
 *
 * [Ambient]s by their nature are hierarchical. They make sense when the value of the ambient needs
 * to be scoped to a particular sub-hierarchy of the composition.
 *
 * One must create an Ambient instance, which can be referenced by the consumers statically. Ambient
 * instances themselves hold no data, and can be thought of as a type-safe identifier for the data
 * being passed down a tree. Ambient factory functions takes a single parameter, a factory to
 * create a default value in cases where an ambient is used without a Provider. If this is a
 * situation you would rather not handle, you can throw an error in this factory
 *
 * @sample androidx.compose.samples.createAmbient
 *
 * Somewhere up the tree, a [Providers] component can be used, which provides a value for the
 * ambient. This would often be at the "root" of a tree, but could be anywhere, and can also be
 * used in multiple places to override the provided value for a sub-tree.
 *
 * @sample androidx.compose.samples.ambientProvider
 *
 * Intermediate components do not need to know about the ambient value, and can have zero
 * dependencies on it. For example, `SomeScreen` might look like this:
 *
 * @sample androidx.compose.samples.someScreenSample
 *
 * Finally, a component that wishes to consume the ambient value can use the [current]
 * property of the ambient key which returns the current value of the ambient, and subscribes the
 * component to changes of it.
 *
 * @sample androidx.compose.samples.consumeAmbient
 */
@Immutable
sealed class Ambient<T> constructor(defaultFactory: (() -> T)? = null) {
    @Suppress("UNCHECKED_CAST")
    internal val defaultValueHolder = LazyValueHolder(defaultFactory)

    @Composable
    internal abstract fun provided(value: T): State<T>

    /**
     * Return the value provided by the nearest [Providers] component that invokes, directly or
     * indirectly, the composable function that uses this property.
     *
     * @sample androidx.compose.samples.consumeAmbient
     */
    @Composable
    inline val current: T get() = currentComposerNonNull.consume(this)
}

/**
 * A [ProvidableAmbient] can be used in [Providers] to provide values.
 *
 * @see ambientOf
 * @see Ambient
 * @see Providers
 */
@Immutable
abstract class ProvidableAmbient<T> internal constructor(defaultFactory: (() -> T)?) :
    Ambient<T> (defaultFactory) {

    /**
     * Associates an ambient key to a value in a call to [Providers].
     *
     * @see Ambient
     * @see ProvidableAmbient
     */
    @Suppress("UNCHECKED_CAST")
    infix fun provides(value: T) = ProvidedValue(this, value)
}

/**
 * A [DynamicProvidableAmbient] is an ambient backed by [mutableStateOf]. Providing new values
 * using a [DynamicProvidableAmbient] will provide the same [State] with a different value.
 * Reading the ambient value of a [DynamicProvidableAmbient] will record a read in the
 * [RecomposeScope] of the composition. Changing the provided value will invalidate the
 * [RecomposeScope]s.
 *
 * @see ambientOf
 */
internal class DynamicProvidableAmbient<T> constructor(defaultFactory: (() -> T)?) :
    ProvidableAmbient<T>(defaultFactory) {

    @Composable
    override fun provided(value: T): State<T> = state { value }.apply { this.value = value }
}

/**
 * A [StaticProvidableAmbient] is a value that is expected to rarely change.
 *
 * @see staticAmbientOf
 */
@Immutable
internal class StaticProvidableAmbient<T>(defaultFactory: (() -> T)?) :
    ProvidableAmbient<T>(defaultFactory) {

    @Composable
    override fun provided(value: T): State<T> = StaticValueHolder(value)
}

/**
 * Create an ambient key that can be provided using [Providers]. Changing the value provided
 * during recomposition will invalidate the children of [Providers] that read the value using
 * [Ambient.current].
 *
 * @see Ambient
 * @see staticAmbientOf
 */
fun <T> ambientOf(defaultFactory: (() -> T)? = null): ProvidableAmbient<T> =
    DynamicProvidableAmbient(defaultFactory)

/**
 * Create an ambient key that can be provided using [Providers]. Changing the value provided
 * will cause the entire tree below [Providers] to be recomposed, disabling skipping of composable
 * calls.
 *
 * A static ambient should be only be used when the value provided is highly unlikely to change.
 *
 * @see Ambient
 * @see ambientOf
 */
fun <T> staticAmbientOf(defaultFactory: (() -> T)? = null): ProvidableAmbient<T> =
    StaticProvidableAmbient(defaultFactory)

/**
 * [Providers] binds values to [ProvidableAmbient] keys. Reading the ambient using
 * [Ambient.current] will return the value provided in [Providers]'s [values] parameter for all
 * composable functions called directly or indirectly in the [children] lambda.
 *
 * @sample androidx.compose.samples.ambientProvider
 *
 * @see Ambient
 * @see ambientOf
 * @see staticAmbientOf
 */
@Composable
fun Providers(vararg values: ProvidedValue<*>, children: @Composable() () -> Unit) {
    with(currentComposerNonNull) {
        startProviders(values)
        children()
        endProviders()
    }
}
