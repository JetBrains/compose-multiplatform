/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.inspection.inspector

import android.annotation.SuppressLint
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.jvm.internal.FunctionBase
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.Lambda
import kotlin.jvm.internal.MutablePropertyReference0
import kotlin.jvm.internal.MutablePropertyReference1
import kotlin.jvm.internal.MutablePropertyReference2
import kotlin.jvm.internal.PropertyReference0
import kotlin.jvm.internal.PropertyReference1
import kotlin.jvm.internal.PropertyReference2
import kotlin.jvm.internal.Reflection
import kotlin.jvm.internal.ReflectionFactory
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.jvm.internal.ReflectionFactoryImpl

/**
 * Scope that allows to use jarjar-ed kotlin-reflect artifact that is shipped with inspector itself.
 *
 * Issue with kotlin-reflect.
 * Many of reflective calls such as "foo::class" rely on static
 * functions defined in kotlin-stdlib's Reflection.java that delegate to ReflectionFactory.
 * In order to initialize that factory kotlin-stdlib statically detects presence or absence of
 * kotlin-reflect in classloader and chooses a factory accordingly. If there is no kotlin-reflect,
 * very limited version of ReflectionFactory is used.
 *
 * It is an issue for inspectors because they could be loaded after that factory is initialised,
 * and even if they are loaded before, they live in a separate child classloader, thus
 * kotlin-reflect in inspector wouldn't exist for kotlin-stdlib in app.
 *
 * First step to avoid the issue is using ReflectionFactoryImpl that is bundled with inspector.
 * Code for that would be fairly simple, for example instead of directly calling
 * `kClass.declaredMemberProperties`, correct instance of kClass should be obtained from factory:
 * `factory.getOrCreateKotlinClass(kClass.java).declaredMemberProperties`.
 *
 * That would work if code that works with correct KClass full implementation would never try to
 * access a default factory installed in Reflection.java. Unfortunately it is not true,
 * it eventually calls `CallableReference.getOwner()` in stdlib that uses default factory.
 *
 * As a result we have to replace the factory in Reflection.java. To avoid issues with user's code
 * factory that we setup is smart, by default it simply delegates to a factory that was previously
 * installed. Only within `reflectionScope.withReflectiveAccess{ }` factory from kotlin-reflect
 * is used.
 */
@SuppressLint("BanUncheckedReflection")
class ReflectionScope {

    companion object {
        init {
            allowHiddenApi()
        }
    }

    private val scopedReflectionFactory = installScopedReflectionFactory()

    /**
     * Runs `block` with access to kotlin-reflect.
     */
    fun <T> withReflectiveAccess(block: () -> T): T {
        return scopedReflectionFactory.withMainFactory(block)
    }

    private fun installScopedReflectionFactory(): ScopedReflectionFactory {
        val factoryField = Reflection::class.java.getDeclaredField("factory")
        factoryField.isAccessible = true
        val original: ReflectionFactory = factoryField.get(null) as ReflectionFactory
        val modifiersField: Field = Field::class.java.getDeclaredField("accessFlags")
        modifiersField.isAccessible = true
        // make field non-final 😅 b/179685774 https://youtrack.jetbrains.com/issue/KT-44795
        modifiersField.setInt(factoryField, factoryField.modifiers and Modifier.FINAL.inv())
        val scopedReflectionFactory = ScopedReflectionFactory(original)
        factoryField.set(null, scopedReflectionFactory)
        return scopedReflectionFactory
    }
}

@SuppressLint("BanUncheckedReflection")
private fun allowHiddenApi() {
    try {
        val vmDebug = Class.forName("dalvik.system.VMDebug")
        val allowHiddenApiReflectionFrom =
            vmDebug.getDeclaredMethod("allowHiddenApiReflectionFrom", Class::class.java)
        allowHiddenApiReflectionFrom.invoke(null, ReflectionScope::class.java)
    } catch (e: Throwable) {
        // ignore failure, let's try to proceed without it
    }
}

private class ScopedReflectionFactory(
    private val original: ReflectionFactory,
) : ReflectionFactory() {
    private val mainFactory = ReflectionFactoryImpl()
    private val threadLocalFactory = ThreadLocal<ReflectionFactory>()

    fun <T> withMainFactory(block: () -> T): T {
        threadLocalFactory.set(mainFactory)
        try {
            return block()
        } finally {
            threadLocalFactory.set(null)
        }
    }

    val factory: ReflectionFactory
        get() = threadLocalFactory.get() ?: original

    override fun createKotlinClass(javaClass: Class<*>?): KClass<*> {
        return factory.createKotlinClass(javaClass)
    }

    override fun createKotlinClass(javaClass: Class<*>?, internalName: String?): KClass<*> {
        return factory.createKotlinClass(javaClass, internalName)
    }

    override fun getOrCreateKotlinPackage(
        javaClass: Class<*>?,
        moduleName: String?
    ): KDeclarationContainer {
        return factory.getOrCreateKotlinPackage(javaClass, moduleName)
    }

    override fun getOrCreateKotlinClass(javaClass: Class<*>?): KClass<*> {
        return factory.getOrCreateKotlinClass(javaClass)
    }

    override fun getOrCreateKotlinClass(javaClass: Class<*>?, internalName: String?): KClass<*> {
        return factory.getOrCreateKotlinClass(javaClass, internalName)
    }

    override fun renderLambdaToString(lambda: Lambda<*>?): String {
        return factory.renderLambdaToString(lambda)
    }

    override fun renderLambdaToString(lambda: FunctionBase<*>?): String {
        return factory.renderLambdaToString(lambda)
    }

    override fun function(f: FunctionReference?): KFunction<*> {
        return factory.function(f)
    }

    override fun property0(p: PropertyReference0?): KProperty0<*> {
        return factory.property0(p)
    }

    override fun mutableProperty0(p: MutablePropertyReference0?): KMutableProperty0<*> {
        return factory.mutableProperty0(p)
    }

    override fun property1(p: PropertyReference1?): KProperty1<*, *> {
        return factory.property1(p)
    }

    override fun mutableProperty1(p: MutablePropertyReference1?): KMutableProperty1<*, *> {
        return factory.mutableProperty1(p)
    }

    override fun property2(p: PropertyReference2?): KProperty2<*, *, *> {
        return factory.property2(p)
    }

    override fun mutableProperty2(p: MutablePropertyReference2?): KMutableProperty2<*, *, *> {
        return factory.mutableProperty2(p)
    }

    override fun typeOf(
        klass: KClassifier?,
        arguments: MutableList<KTypeProjection>?,
        isMarkedNullable: Boolean
    ): KType {
        return factory.typeOf(klass, arguments, isMarkedNullable)
    }

    override fun typeParameter(
        container: Any?,
        name: String?,
        variance: KVariance?,
        isReified: Boolean
    ): KTypeParameter {
        return factory.typeParameter(container, name, variance, isReified)
    }

    override fun setUpperBounds(typeParameter: KTypeParameter?, bounds: MutableList<KType>?) {
        factory.setUpperBounds(typeParameter, bounds)
    }
}