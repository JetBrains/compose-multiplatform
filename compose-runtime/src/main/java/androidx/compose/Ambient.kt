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
 * ```
 *     // One must create an Ambient instance, which can be referenced by the consumers
 *     // statically. Ambient instances themselves hold no data, and can be thought of as a
 *     // type-safe identifier for the data being passed down a tree. The Ambient constructor takes a
 *     // single parameter, a factory to create a default value in cases where an ambient is
 *     // used without a Provider. If this is a situation you would rather not handle, you
 *     // can throw an error in this factory
 *     val ActiveUser = Ambient.of<User> { error("No active user found!") }
 *
 *     // Somewhere up the tree, a "Provider" component can be used, which has a required
 *     // "value" parameter. This would often be at the "root" of a tree, but could be
 *     // anywhere, and can also be used in multiple places to override the provided value
 *     // for a sub-tree.
 *     @Composable fun App(user: User) {
 *         ActiveUser.Provider(value=user) {
 *              SomeScreen()
 *         }
 *     }
 *
 *     // intermediate components do not need to know about the ambient value, and can have
 *     // zero dependencies on it
 *     @Composable fun SomeScreen() {
 *         UserPhoto()
 *     }
 *
 *     // A component that wishes to consume the ambient value can use the corresponding
 *     // "ambient" effect, which returns the current value of the ambient, and subscribes the component
 *     // to changes of it
 *     @Composable fun UserPhoto() {
 *         val user = +ambient(ActiveUser)
 *         ImageView(src=user.profilePhotoUrl)
 *     }
 * ```
 *
 * @see [ambient]
 */
class Ambient<T>
@PublishedApi
internal constructor(private val key: String, private val defaultFactory: (() -> T)? = null) {
    @Suppress("UNCHECKED_CAST")
    internal val defaultValue by lazy {
        val fn = defaultFactory
        if (fn != null) fn()
        else null as T
    }

    companion object {
        /**
         * Creates an ambient to be used during composition.
         *
         * @param key A string identifier used to disambiguate the Ambient.
         *  Defaults to the name of [T].
         * @param defaultFactory A lambda to run to create a default value of this ambient for when
         *  the ambient is consumed in a composition and there is no provider above the point of
         *  consumption. If no factory is provided, and [T] is not a nullable type, an exception
         *  will be thrown. This factory will not be executed more than once.
         */
        inline fun <reified T> of(
            key: String = T::class.java.simpleName,
            noinline defaultFactory: (() -> T)? = null
        ) = Ambient(key, defaultFactory)
    }

    override fun hashCode() = key.hashCode()
    override fun equals(other: Any?) = this === other
    override fun toString(): String = "Ambient<$key>"

    internal class Holder<T>(val ambient: Ambient<T>, var value: T)

    /**
     * The Provider component allows one to provide an ambient value to a section of the tree during
     * composition. All consumers of the ambient that are underneath this component will receive the
     * value provided here. Consuming components are guaranteed to be invalidated and recomposed
     * when this value changes.
     *
     * @param value The current value of the ambient.
     * @param children Everything composed inside this block will get [value] when consuming this
     *  ambient
     *
     * @see ambient
     */
    @Composable
    fun Provider(
        value: T, @Children
        children: @Composable() () -> Unit
    ) {
        with(currentComposerNonNull) {
            val holder = +memo {
                Holder(
                    this@Ambient,
                    value
                )
            }
            holder.value = value
            startProvider(holder, value)
            @Suppress("PLUGIN_ERROR")
            children()
            endProvider()
        }
    }
}
