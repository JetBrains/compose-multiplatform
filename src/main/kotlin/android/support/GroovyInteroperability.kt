/*
 * Copyright 2016 the original author or authors.
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

package android.support

import groovy.lang.Closure
import groovy.lang.GroovyObject
import groovy.lang.MetaClass

import org.codehaus.groovy.runtime.InvokerHelper.getMetaClass

operator fun <T> Closure<T>.invoke(): T = call()

operator fun <T> Closure<T>.invoke(x: Any?): T = call(x)

operator fun <T> Closure<T>.invoke(vararg xs: Any?): T = call(*xs)


/**
 * Executes the given [builder] against this object's [GroovyBuilderScope].
 *
 * @see [GroovyBuilderScope]
 */
inline
fun <T> Any.withGroovyBuilder(builder: GroovyBuilderScope.() -> T): T =
        GroovyBuilderScope.of(this).builder()


/**
 * Provides a dynamic dispatching DSL with Groovy semantics for better integration with
 * plugins that rely on Groovy builders such as the core `maven` plugin.
 *
 * It supports Groovy keyword arguments and arbitrary nesting, for instance, the following Groovy code:
 *
 * ```Groovy
 * repository(url: "scp://repos.mycompany.com/releases") {
 *   authentication(userName: "me", password: "myPassword")
 * }
 * ```
 *
 * Can be mechanically translated to the following Kotlin with the aid of `withGroovyBuilder`:
 *
 * ```Kotlin
 * withGroovyBuilder {
 *   "repository"("url" to "scp://repos.mycompany.com/releases") {
 *     "authentication"("userName" to "me", "password" to "myPassword")
 *   }
 * }
 * ```
 *
 * @see [withGroovyBuilder]
 */
interface GroovyBuilderScope : GroovyObject {

    companion object {

        fun of(value: Any): GroovyBuilderScope =
                when (value) {
                    is GroovyObject -> GroovyBuilderScopeForGroovyObject(value)
                    else            -> GroovyBuilderScopeForRegularObject(value)
                }
    }

    val delegate: Any

    operator fun String.invoke(vararg arguments: Any?): Any?

    operator fun String.invoke(): Any? =
            invoke(*emptyArray<Any>())

    operator fun <T> String.invoke(vararg arguments: Any?, builder: GroovyBuilderScope.() -> T): Any? =
            invoke(*arguments, closureFor(builder))

    operator fun <T> String.invoke(builder: GroovyBuilderScope.() -> T): Any? =
            invoke(closureFor(builder))

    operator fun <T> String.invoke(vararg keywordArguments: Pair<String, Any?>, builder: GroovyBuilderScope.() -> T): Any? =
            invoke(keywordArguments.toMap(), closureFor(builder))

    operator fun String.invoke(vararg keywordArguments: Pair<String, Any?>): Any? =
            invoke(keywordArguments.toMap())

    private
    fun <T> closureFor(builder: GroovyBuilderScope.() -> T): Closure<Any?> =
            object : Closure<Any?>(this, this) {
                @Suppress("unused")
                fun doCall() = delegate.withGroovyBuilder(builder)
            }
}


private
class GroovyBuilderScopeForGroovyObject(override val delegate: GroovyObject) : GroovyBuilderScope, GroovyObject by delegate {

    override fun String.invoke(vararg arguments: Any?): Any? =
            delegate.invokeMethod(this, arguments)
}


private
class GroovyBuilderScopeForRegularObject(override val delegate: Any) : GroovyBuilderScope {

    private
    val groovyMetaClass: MetaClass by lazy {
        getMetaClass(delegate)
    }

    override fun invokeMethod(name: String, args: Any?): Any? =
            groovyMetaClass.invokeMethod(delegate, name, args)

    override fun setProperty(propertyName: String, newValue: Any?) =
            groovyMetaClass.setProperty(delegate, propertyName, newValue)

    override fun getProperty(propertyName: String): Any =
            groovyMetaClass.getProperty(delegate, propertyName)

    override fun setMetaClass(metaClass: MetaClass?) =
            throw IllegalStateException()

    override fun getMetaClass(): MetaClass =
            groovyMetaClass

    override fun String.invoke(vararg arguments: Any?): Any? =
            groovyMetaClass.invokeMethod(delegate, this, arguments)
}