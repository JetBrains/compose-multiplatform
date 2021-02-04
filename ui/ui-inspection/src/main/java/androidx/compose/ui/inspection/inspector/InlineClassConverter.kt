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

package androidx.compose.ui.inspection.inspector

/**
 * Converter for casting a parameter represented by its primitive value to its inline class type.
 *
 * For example: an androidx.compose.ui.graphics.Color instance is often represented by a long
 */
internal class InlineClassConverter {
    // Map from inline type name to inline class and conversion lambda
    private val typeMap = mutableMapOf<String, (Any) -> Any>()
    // Return value used in functions
    private val notInlineType: (Any) -> Any = { it }

    /**
     * Clear any cached data.
     */
    fun clear() {
        typeMap.clear()
    }

    /**
     * Cast the specified [value] to a value of type [inlineClassName] if possible.
     *
     * @param inlineClassName the fully qualified name of the inline class.
     * @param value the value to convert to an instance of [inlineClassName].
     */
    fun castParameterValue(inlineClassName: String?, value: Any?): Any? =
        if (value != null && inlineClassName != null)
            typeMapperFor(inlineClassName)(value) else value

    private fun typeMapperFor(typeName: String): (Any) -> (Any) =
        typeMap.getOrPut(typeName) { loadTypeMapper(typeName.replace('.', '/')) }

    private fun loadTypeMapper(className: String): (Any) -> Any {
        val javaClass = loadClassOrNull(className) ?: return notInlineType
        val create = javaClass.declaredConstructors.singleOrNull() ?: return notInlineType
        create.isAccessible = true
        return { value -> create.newInstance(value) }
    }

    private fun loadClassOrNull(className: String): Class<*>? =
        try {
            javaClass.classLoader!!.loadClass(className)
        } catch (ex: Exception) {
            null
        }
}
