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

package androidx.compose.runtime

/**
 * Compose passes data through the composition tree explicitly through means of parameters to
 * composable functions. This is often times the simplest and best way to have data flow through
 * the tree.
 *
 * Sometimes this model can be cumbersome or break down for data that is needed by lots of
 * components, or when components need to pass data between one another but keep that implementation
 * detail private. For these cases, [CompositionLocal]s can be used as an implicit way to have data
 * flow through a composition.
 *
 * [CompositionLocal]s by their nature are hierarchical. They make sense when the value of the
 * [CompositionLocal] needs to be scoped to a particular sub-hierarchy of the composition.
 *
 * One must create a [CompositionLocal] instance, which can be referenced by the consumers
 * statically. [CompositionLocal] instances themselves hold no data, and can be thought of as a
 * type-safe identifier for the data being passed down a tree. [CompositionLocal] factory functions
 * take a single parameter: a factory to create a default value in cases where a [CompositionLocal]
 * is used without a Provider. If this is a situation you would rather not handle, you can throw
 * an error in this factory.
 *
 * @sample androidx.compose.runtime.samples.createCompositionLocal
 *
 * Somewhere up the tree, a [CompositionLocalProvider] component can be used, which provides a
 * value for the [CompositionLocal]. This would often be at the "root" of a tree, but could be
 * anywhere, and can also be used in multiple places to override the provided value for a sub-tree.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 *
 * Intermediate components do not need to know about the [CompositionLocal] value, and can have zero
 * dependencies on it. For example, `SomeScreen` might look like this:
 *
 * @sample androidx.compose.runtime.samples.someScreenSample
 *
 * Finally, a component that wishes to consume the [CompositionLocal] value can use the [current]
 * property of the [CompositionLocal] key which returns the current value of the
 * [CompositionLocal], and subscribes the component to changes of it.
 *
 * @sample androidx.compose.runtime.samples.consumeCompositionLocal
 */
@Stable
sealed class CompositionLocal<T> constructor(defaultFactory: () -> T) {
    @Suppress("UNCHECKED_CAST")
    internal val defaultValueHolder = LazyValueHolder(defaultFactory)

    @Composable
    internal abstract fun provided(value: T): State<T>

    /**
     * Return the value provided by the nearest [CompositionLocalProvider] component that invokes, directly or
     * indirectly, the composable function that uses this property.
     *
     * @sample androidx.compose.runtime.samples.consumeCompositionLocal
     */
    @OptIn(InternalComposeApi::class)
    inline val current: T
        @ReadOnlyComposable
        @Composable
        get() = currentComposer.consume(this)
}

/**
 * A [ProvidableCompositionLocal] can be used in [CompositionLocalProvider] to provide values.
 *
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 * @see CompositionLocal
 * @see CompositionLocalProvider
 */
@Stable
abstract class ProvidableCompositionLocal<T> internal constructor(defaultFactory: () -> T) :
    CompositionLocal<T> (defaultFactory) {

    /**
     * Associates a [CompositionLocal] key to a value in a call to [CompositionLocalProvider].
     *
     * @see CompositionLocal
     * @see ProvidableCompositionLocal
     */
    @Suppress("UNCHECKED_CAST")
    infix fun provides(value: T) = ProvidedValue(this, value, true)

    /**
     * Associates a [CompositionLocal] key to a value in a call to [CompositionLocalProvider] if the key does not
     * already have an associated value.
     *
     * @see CompositionLocal
     * @see ProvidableCompositionLocal
     */
    @Suppress("UNCHECKED_CAST")
    infix fun providesDefault(value: T) = ProvidedValue(this, value, false)
}

/**
 * A [DynamicProvidableCompositionLocal] is a [CompositionLocal] backed by [mutableStateOf].
 * Providing new values using a [DynamicProvidableCompositionLocal] will provide the same [State]
 * with a different value. Reading the [CompositionLocal] value of a
 * [DynamicProvidableCompositionLocal] will record a read in the [RecomposeScope] of the
 * composition. Changing the provided value will invalidate the [RecomposeScope]s.
 *
 * @see compositionLocalOf
 */
internal class DynamicProvidableCompositionLocal<T> constructor(
    private val policy: SnapshotMutationPolicy<T>,
    defaultFactory: () -> T
) : ProvidableCompositionLocal<T>(defaultFactory) {

    @Composable
    override fun provided(value: T): State<T> = remember { mutableStateOf(value, policy) }.apply {
        this.value = value
    }
}

/**
 * A [StaticProvidableCompositionLocal] is a value that is expected to rarely change.
 *
 * @see staticCompositionLocalOf
 */
internal class StaticProvidableCompositionLocal<T>(defaultFactory: () -> T) :
    ProvidableCompositionLocal<T>(defaultFactory) {

    @Composable
    override fun provided(value: T): State<T> = StaticValueHolder(value)
}

/**
 * Create a [CompositionLocal] key that can be provided using [CompositionLocalProvider].
 * Changing the value provided during recomposition will invalidate the content of
 * [CompositionLocalProvider] that read the value using [CompositionLocal.current].
 *
 * [compositionLocalOf] creates a [ProvidableCompositionLocal] which can be used in a a call to
 * [CompositionLocalProvider]. Similar to [MutableList] vs. [List], if the key is made public
 * as [CompositionLocal] instead of [ProvidableCompositionLocal], it can be read using
 * [CompositionLocal.current] but not re-provided.
 *
 * @param policy a policy to determine when a [CompositionLocal] is considered changed. See
 * [SnapshotMutationPolicy] for details.
 * @param defaultFactory a value factory to supply a value when a value is not provided. This
 * factory is called when no value is provided through a [CompositionLocalProvider] of the caller
 * of the component using [CompositionLocal.current]. If no reasonable default can be provided then
 * consider throwing an exception.
 *
 * @see CompositionLocal
 * @see staticCompositionLocalOf
 * @see mutableStateOf
 */
fun <T> compositionLocalOf(
    policy: SnapshotMutationPolicy<T> =
        structuralEqualityPolicy(),
    defaultFactory: () -> T
): ProvidableCompositionLocal<T> = DynamicProvidableCompositionLocal(policy, defaultFactory)

/**
 * Create a [CompositionLocal] key that can be provided using [CompositionLocalProvider].
 *
 * Unlike [compositionLocalOf], reads of a [staticCompositionLocalOf] are not tracked by the
 * composer and changing the value provided in the [CompositionLocalProvider] call will cause the
 * entirety of the content to be recomposed instead of just the places where in the composition the
 * local value is used. This lack of tracking, however, makes a [staticCompositionLocalOf] more
 * efficient when the value provided is highly unlikely to or will never change. For example,
 * the android context, font loaders, or similar shared values, are unlikely to change for the
 * components in the content of a the [CompositionLocalProvider] and should consider using a
 * [staticCompositionLocalOf]. A color, or other theme like value, might change or even be
 * animated therefore a [compositionLocalOf] should be used.
 *
 * [staticCompositionLocalOf] creates a [ProvidableCompositionLocal] which can be used in a a
 * call to [CompositionLocalProvider]. Similar to [MutableList] vs. [List], if the key is made
 * public as [CompositionLocal] instead of [ProvidableCompositionLocal], it can be read using
 * [CompositionLocal.current] but not re-provided.
 *
 * @param defaultFactory a value factory to supply a value when a value is not provided. This
 * factory is called when no value is provided through a [CompositionLocalProvider] of the caller
 * of the component using [CompositionLocal.current]. If no reasonable default can be provided then
 * consider throwing an exception.
 *
 * @see CompositionLocal
 * @see compositionLocalOf
 */
fun <T> staticCompositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> =
    StaticProvidableCompositionLocal(defaultFactory)

/**
 * [CompositionLocalProvider] binds values to [ProvidableCompositionLocal] keys. Reading the
 * [CompositionLocal] using [CompositionLocal.current] will return the value provided in
 * [CompositionLocalProvider]'s [values] parameter for all composable functions called directly
 * or indirectly in the [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 *
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
@OptIn(InternalComposeApi::class)
fun CompositionLocalProvider(vararg values: ProvidedValue<*>, content: @Composable () -> Unit) {
    currentComposer.startProviders(values)
    content()
    currentComposer.endProviders()
}
