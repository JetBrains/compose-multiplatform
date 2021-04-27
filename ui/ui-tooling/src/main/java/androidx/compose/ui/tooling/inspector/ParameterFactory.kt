/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.tooling.inspector

import android.util.Log
import android.view.View
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.internal.ComposableLambda
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.inspector.ParameterType.DimensionDp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import java.lang.reflect.Field
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.Lambda
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import java.lang.reflect.Modifier as JavaModifier

private const val MAX_RECURSIONS = 10
private const val MAX_ITERABLE = 25

/**
 * Factory of [NodeParameter]s.
 *
 * Each parameter value is converted to a user readable value.
 */
internal class ParameterFactory(private val inlineClassConverter: InlineClassConverter) {
    /**
     * A map from known values to a user readable string representation.
     */
    private val valueLookup = mutableMapOf<Any, String>()

    /**
     * The classes we have loaded constants from.
     */
    private val valuesLoaded = mutableSetOf<Class<*>>()

    /**
     * Do not load constant names from instances of these classes.
     * We prefer showing the raw values of Color and Dimensions.
     */
    private val ignoredClasses = listOf(Color::class.java, Dp::class.java)
    private var creatorCache: ParameterCreator? = null
    private val kotlinReflectionSupported = try {
        Class.forName("kotlin.reflect.full.KClasses")
        true
    } catch (ex: Exception) {
        false
    }

    /**
     * Do not decompose instances or lookup constants from these package prefixes
     *
     * The following instances are known to contain self recursion:
     * - kotlinx.coroutines.flow.StateFlowImpl
     * - androidx.compose.ui.node.LayoutNode
     */
    private val ignoredPackagePrefixes = listOf(
        "android.", "java.", "javax.", "kotlinx.", "androidx.compose.ui.node."
    )

    var density = Density(1.0f)

    init {
        val textDecorationCombination = TextDecoration.combine(
            listOf(TextDecoration.LineThrough, TextDecoration.Underline)
        )
        valueLookup[textDecorationCombination] = "LineThrough+Underline"
        valueLookup[Color.Unspecified] = "Unspecified"
        valueLookup[RectangleShape] = "RectangleShape"
        valuesLoaded.add(Enum::class.java)
        valuesLoaded.add(Any::class.java)

        // AbsoluteAlignment is not found from an instance of BiasAbsoluteAlignment,
        // because Alignment has no file level class.
        loadConstantsFromEnclosedClasses(AbsoluteAlignment::class.java)
    }

    /**
     * Create a [NodeParameter] from the specified parameter [name] and [value].
     *
     * Attempt to convert the value to a user readable value.
     * For now: return null when a conversion is not possible/found.
     */
    fun create(node: InspectorNode, name: String, value: Any?): NodeParameter? {
        val creator = creatorCache ?: ParameterCreator()
        try {
            return creator.create(node, name, value)
        } finally {
            creatorCache = creator
        }
    }

    private fun loadConstantsFrom(javaClass: Class<*>) {
        if (valuesLoaded.contains(javaClass) ||
            ignoredPackagePrefixes.any { javaClass.name.startsWith(it) }
        ) {
            return
        }
        val related = generateSequence(javaClass) { it.superclass }.plus(javaClass.interfaces)
        related.forEach { aClass ->
            val topClass = generateSequence(aClass) { safeEnclosingClass(it) }.last()
            loadConstantsFromEnclosedClasses(topClass)
            findPackageLevelClass(topClass)?.let { loadConstantsFromStaticFinal(it) }
        }
    }

    private fun safeEnclosingClass(klass: Class<*>): Class<*>? = try {
        klass.enclosingClass
    } catch (_: Error) {
        // Exceptions seen on API 23...
        null
    }

    private fun findPackageLevelClass(javaClass: Class<*>): Class<*>? = try {
        // Note: This doesn't work when @file.JvmName is specified
        Class.forName("${javaClass.name}Kt")
    } catch (ex: Throwable) {
        null
    }

    private fun loadConstantsFromEnclosedClasses(javaClass: Class<*>) {
        if (valuesLoaded.contains(javaClass)) {
            return
        }
        loadConstantsFromObjectInstance(javaClass.kotlin)
        loadConstantsFromStaticFinal(javaClass)
        valuesLoaded.add(javaClass)
        javaClass.declaredClasses.forEach { loadConstantsFromEnclosedClasses(it) }
    }

    /**
     * Load all constants from companion objects and singletons
     *
     * Exclude: primary types and types of ignoredClasses, open and lateinit vals.
     */
    private fun loadConstantsFromObjectInstance(kClass: KClass<*>) {
        try {
            val instance = kClass.objectInstance ?: return
            kClass.declaredMemberProperties.asSequence()
                .filter { it.isFinal && !it.isLateinit }
                .mapNotNull { constantValueOf(it, instance)?.let { key -> Pair(key, it.name) } }
                .filter { !ignoredValue(it.first) }
                .toMap(valueLookup)
        } catch (_: Throwable) {
            // KT-16479 :  kotlin reflection does currently not support packages and files.
            // We load top level values using Java reflection instead.
            // Ignore other reflection errors as well
        }
    }

    /**
     * Load all constants from top level values from Java.
     *
     * Exclude: primary types and types of ignoredClasses.
     * Since this is Java, inline types will also (unfortunately) be excluded.
     */
    private fun loadConstantsFromStaticFinal(javaClass: Class<*>) {
        try {
            javaClass.declaredMethods.asSequence()
                .filter {
                    it.returnType != Void.TYPE &&
                        JavaModifier.isStatic(it.modifiers) &&
                        JavaModifier.isFinal(it.modifiers) &&
                        !it.returnType.isPrimitive &&
                        it.parameterTypes.isEmpty() &&
                        it.name.startsWith("get")
                }
                .mapNotNull { javaClass.getDeclaredField(it.name.substring(3)) }
                .mapNotNull { constantValueOf(it)?.let { key -> Pair(key, it.name) } }
                .filter { !ignoredValue(it.first) }
                .toMap(valueLookup)
        } catch (_: ReflectiveOperationException) {
            // ignore reflection errors
        } catch (_: NoClassDefFoundError) {
            // ignore missing classes on lower level SDKs
        }
    }

    private fun constantValueOf(field: Field?): Any? = try {
        field?.isAccessible = true
        field?.get(null)
    } catch (_: ReflectiveOperationException) {
        // ignore reflection errors
        null
    }

    private fun constantValueOf(property: KProperty1<out Any, *>, instance: Any): Any? = try {
        val field = property.javaField
        field?.isAccessible = true
        inlineClassConverter.castParameterValue(inlineResultClass(property), field?.get(instance))
    } catch (_: ReflectiveOperationException) {
        // ignore reflection errors
        null
    }

    private fun inlineResultClass(property: KProperty1<out Any, *>): String? {
        // The Java getter name will be mangled if it contains parameters of an inline class.
        // The mangled part starts with a '-'.
        if (property.javaGetter?.name?.contains('-') == true) {
            return property.returnType.toString()
        }
        return null
    }

    private fun ignoredValue(value: Any?): Boolean =
        value == null ||
            ignoredClasses.any { ignored -> ignored.isInstance(value) } ||
            value::class.java.isPrimitive

    /**
     * Convenience class for building [NodeParameter]s.
     */
    private inner class ParameterCreator {
        private var node: InspectorNode? = null
        private var recursions = 0

        fun create(node: InspectorNode, name: String, value: Any?): NodeParameter? = try {
            this.node = node
            recursions = 0
            create(name, value)
        } finally {
            this.node = null
        }

        private fun create(name: String, value: Any?): NodeParameter? {
            if (value == null || recursions >= MAX_RECURSIONS) {
                return null
            }
            try {
                recursions++
                createFromConstant(name, value)?.let { return it }
                return when (value) {
                    is AnnotatedString -> NodeParameter(name, ParameterType.String, value.text)
                    is BaselineShift -> createFromBaselineShift(name, value)
                    is Boolean -> NodeParameter(name, ParameterType.Boolean, value)
                    is ComposableLambda -> createFromCLambda(name, value)
                    is Color -> NodeParameter(name, ParameterType.Color, value.toArgb())
                    is CornerSize -> createFromCornerSize(name, value)
                    is Double -> NodeParameter(name, ParameterType.Double, value)
                    is Dp -> NodeParameter(name, DimensionDp, value.value)
                    is Enum<*> -> NodeParameter(name, ParameterType.String, value.toString())
                    is Float -> NodeParameter(name, ParameterType.Float, value)
                    is FunctionReference -> NodeParameter(
                        name, ParameterType.FunctionReference, arrayOf<Any>(value, value.name)
                    )
                    is FontListFontFamily -> createFromFontListFamily(name, value)
                    is FontWeight -> NodeParameter(name, ParameterType.Int32, value.weight)
                    is Modifier -> createFromModifier(name, value)
                    is InspectableValue -> createFromInspectableValue(name, value)
                    is Int -> NodeParameter(name, ParameterType.Int32, value)
                    is Iterable<*> -> createFromIterable(name, value)
                    is Lambda<*> -> createFromLambda(name, value)
                    is Locale -> NodeParameter(name, ParameterType.String, value.toString())
                    is LocaleList ->
                        NodeParameter(name, ParameterType.String, value.localeList.joinToString())
                    is Long -> NodeParameter(name, ParameterType.Int64, value)
                    is Offset -> createFromOffset(name, value)
                    is Shadow -> createFromShadow(name, value)
                    is SolidColor -> NodeParameter(name, ParameterType.Color, value.value.toArgb())
                    is String -> NodeParameter(name, ParameterType.String, value)
                    is TextUnit -> createFromTextUnit(name, value)
                    is ImageVector -> createFromImageVector(name, value)
                    is View -> NodeParameter(name, ParameterType.String, value.javaClass.simpleName)
                    else -> createFromKotlinReflection(name, value)
                }
            } finally {
                recursions--
            }
        }

        private fun createFromBaselineShift(name: String, value: BaselineShift): NodeParameter {
            val converted = when (value.multiplier) {
                BaselineShift.None.multiplier -> "None"
                BaselineShift.Subscript.multiplier -> "Subscript"
                BaselineShift.Superscript.multiplier -> "Superscript"
                else -> return NodeParameter(name, ParameterType.Float, value.multiplier)
            }
            return NodeParameter(name, ParameterType.String, converted)
        }

        private fun createFromCLambda(name: String, value: ComposableLambda): NodeParameter? = try {
            val lambda = value.javaClass.getDeclaredField("_block")
                .apply { isAccessible = true }
                .get(value)
            NodeParameter(name, ParameterType.Lambda, arrayOf<Any?>(lambda))
        } catch (_: Throwable) {
            null
        }

        private fun createFromConstant(name: String, value: Any): NodeParameter? {
            if (!kotlinReflectionSupported) {
                return null
            }
            loadConstantsFrom(value.javaClass)
            return valueLookup[value]?.let { NodeParameter(name, ParameterType.String, it) }
        }

        private fun createFromCornerSize(name: String, value: CornerSize): NodeParameter {
            val size = Size(node!!.width.toFloat(), node!!.height.toFloat())
            val pixels = value.toPx(size, density)
            return NodeParameter(name, DimensionDp, with(density) { pixels.toDp().value })
        }

        // For now: select ResourceFontFont closest to W400 and Normal, and return the resId
        private fun createFromFontListFamily(
            name: String,
            value: FontListFontFamily
        ): NodeParameter? =
            findBestResourceFont(value)?.let {
                NodeParameter(name, ParameterType.Resource, it.resId)
            }

        private fun createFromKotlinReflection(name: String, value: Any): NodeParameter? {
            val kClass = value::class
            val qualifiedName = kClass.qualifiedName
            if (kClass.simpleName == null ||
                qualifiedName == null ||
                ignoredPackagePrefixes.any { qualifiedName.startsWith(it) } ||
                !kotlinReflectionSupported
            ) {
                // Exit without creating a parameter for:
                // - internal synthetic classes
                // - certain android packages
                // - if kotlin reflection library not available
                return null
            }
            val parameter = NodeParameter(name, ParameterType.String, kClass.simpleName)
            val properties = mutableMapOf<String, KProperty1<Any, *>>()
            try {
                sequenceOf(kClass).plus(kClass.allSuperclasses.asSequence())
                    .flatMap { it.declaredMemberProperties.asSequence() }
                    .filterIsInstance<KProperty1<Any, *>>()
                    .associateByTo(properties) { it.name }
            } catch (ex: Throwable) {
                Log.w("Compose", "Could not decompose ${kClass.simpleName}")
                return parameter
            }
            properties.values.mapNotNullTo(parameter.elements) {
                create(it.name, valueOf(it, value))
            }
            return parameter
        }

        private fun valueOf(property: KProperty1<Any, *>, instance: Any): Any? = try {
            property.isAccessible = true
            // Bug in kotlin reflection API: if the type is a nullable inline type with a null
            // value, we get an IllegalArgumentException in this line:
            property.get(instance)
        } catch (ex: Throwable) {
            // TODO: Remove this warning since this is expected with nullable inline types
            Log.w("Compose", "Could not get value of ${property.name}")
            null
        }

        private fun createFromInspectableValue(
            name: String,
            value: InspectableValue
        ): NodeParameter {
            val tempValue = value.valueOverride ?: ""
            val parameterName = name.ifEmpty { value.nameFallback } ?: "element"
            val parameterValue = if (tempValue is InspectableValue) "" else tempValue
            val parameter = create(parameterName, parameterValue)
                ?: NodeParameter(parameterName, ParameterType.String, "")
            val elements = parameter.elements
            value.inspectableElements.mapNotNullTo(elements) { create(it.name, it.value) }
            return parameter
        }

        private fun createFromIterable(name: String, value: Iterable<*>): NodeParameter {
            val parameter = NodeParameter(name, ParameterType.String, "")
            val elements = parameter.elements
            value.asSequence()
                .mapNotNull { create(elements.size.toString(), it) }
                .takeWhile { elements.size < MAX_ITERABLE }
                .toCollection(elements)
            return parameter
        }

        private fun createFromLambda(name: String, value: Lambda<*>): NodeParameter =
            NodeParameter(name, ParameterType.Lambda, arrayOf<Any>(value))

        private fun createFromModifier(name: String, value: Modifier): NodeParameter? =
            when {
                name.isNotEmpty() -> {
                    val parameter = NodeParameter(name, ParameterType.String, "")
                    val elements = parameter.elements
                    value.foldIn(elements) { acc, m ->
                        create("", m)?.let { param -> acc.apply { add(param) } } ?: acc
                    }
                    parameter
                }
                value is InspectableValue -> createFromInspectableValue(name, value)
                else -> null
            }

        private fun createFromOffset(name: String, value: Offset): NodeParameter {
            val parameter = NodeParameter(name, ParameterType.String, Offset::class.java.simpleName)
            val elements = parameter.elements
            elements.add(NodeParameter("x", DimensionDp, with(density) { value.x.toDp().value }))
            elements.add(NodeParameter("y", DimensionDp, with(density) { value.y.toDp().value }))
            return parameter
        }

        // Special handling of blurRadius: convert to dp:
        private fun createFromShadow(name: String, value: Shadow): NodeParameter? {
            val parameter = createFromKotlinReflection(name, value) ?: return null
            val elements = parameter.elements
            val index = elements.indexOfFirst { it.name == "blurRadius" }
            if (index >= 0) {
                val blurRadius = with(density) { value.blurRadius.toDp().value }
                elements[index] = NodeParameter("blurRadius", DimensionDp, blurRadius)
            }
            return parameter
        }

        @Suppress("DEPRECATION")
        private fun createFromTextUnit(name: String, value: TextUnit): NodeParameter =
            when (value.type) {
                TextUnitType.Sp -> NodeParameter(name, ParameterType.DimensionSp, value.value)
                TextUnitType.Em -> NodeParameter(name, ParameterType.DimensionEm, value.value)
                TextUnitType.Unspecified ->
                    NodeParameter(name, ParameterType.String, "Unspecified")
            }

        private fun createFromImageVector(name: String, value: ImageVector): NodeParameter =
            NodeParameter(name, ParameterType.String, value.name)

        /**
         * Select a resource font among the font in the family to represent the font
         *
         * Prefer the font closest to [FontWeight.Normal] and [FontStyle.Normal]
         */
        private fun findBestResourceFont(value: FontListFontFamily): ResourceFont? =
            value.fonts.asSequence().filterIsInstance<ResourceFont>().minByOrNull {
                abs(it.weight.weight - FontWeight.Normal.weight) + it.style.ordinal
            }
    }
}
