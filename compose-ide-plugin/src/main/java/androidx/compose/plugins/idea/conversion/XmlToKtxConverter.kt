/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.plugins.idea.conversion

import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.core.isExcludedFromAutoImport
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.j2k.ast.DeclarationStatement
import org.jetbrains.kotlin.j2k.ast.Element
import org.jetbrains.kotlin.j2k.ast.Expression
import org.jetbrains.kotlin.j2k.ast.KtxAttribute
import org.jetbrains.kotlin.j2k.ast.KtxElement
import org.jetbrains.kotlin.j2k.ast.LiteralExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import androidx.compose.plugins.idea.AttributeInfo
import androidx.compose.plugins.idea.AttributeInfoExtractor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class XmlToKtxConverter(private val targetFile: KtFile) {
    companion object {
        private const val NAMESPACE_PREFIX = "xmlns"
        private val NON_ALPHANUMERIC_PATTERN = Regex("[^A-Za-z0-9]")

        private val CLASS_PREFIX_LIST = listOf(
            "android.widget.",
            "android.webkit.",
            "android.app.",
            "android.view."
        )

        private enum class SpecialTags(val tagName: String) {
            VIEW("view"),
            BLINK("blink"),
            MERGE("merge"),
            INCLUDE("include"),
            REQUEST_FOCUS("requestFocus"),
            TAG("tag")
        }

        enum class XmlNamespace(val uri: String, val defaultPrefix: String) {
            ANDROID("http://schemas.android.com/apk/res/android", "android"),
            ANDROID_TOOLS("http://schemas.android.com/tools", "tools"),
            ANDROID_APP("http://schemas.android.com/apk/res-auto", "app");
        }

        fun formatCode(file: PsiFile, range: RangeMarker? = null) {
            runWriteAction {
                val codeStyleManager = CodeStyleManager.getInstance(file.project)
                if (range != null && range.isValid) {
                    codeStyleManager.reformatRange(file, range.startOffset, range.endOffset)
                } else {
                    codeStyleManager.reformat(file)
                }
            }
        }
    }

    private val project = targetFile.project
    // TODO(jdemeulenaere): Compare with `isVisibleDescriptor` in (BaseCompose)CompletionSession to add more checks here.
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean = lambda@{ descriptor ->
        if (descriptor is DeclarationDescriptorWithVisibility) {
            return@lambda descriptor.isVisible(targetFile.findModuleDescriptor())
        }

        return@lambda descriptor.isExcludedFromAutoImport(project, targetFile)
    }
    private val attributeInfoExtractor =
        AttributeInfoExtractor(targetFile, visibilityFilter)

    fun convertElement(element: PsiElement): Element? = when (element) {
        is XmlFile -> convertFile(element)
        is XmlTag -> convertTag(element)
        // We return an empty element as `maybeConvertAttribute` returns null if the attribute should be skipped.
        is XmlAttribute -> maybeConvertAttribute(element) ?: Element.Empty
        else -> null
    }

    private fun convertFile(element: XmlFile): Element {
        return element.rootTag?.let { convertTag(it) } ?: Element.Empty
    }

    private val valueConversionsCache = hashMapOf<Pair<String, String>, List<ValueConversion>>()
    private fun getValueConversions(
        tagQualifiedName: String,
        xmlAttributeName: String
    ): List<ValueConversion> {
        return valueConversionsCache.computeIfAbsent(Pair(tagQualifiedName, xmlAttributeName)) {
            val descriptor = findClass(tagQualifiedName)?.getJavaClassDescriptor()
                ?: return@computeIfAbsent emptyList()
            val classNames = descriptor.getAllSuperClassifiers().toList().mapNotNull {
                it.fqNameOrNull()?.asString()
            }.toSet()
            ANDROID_CONVERSION.classConversions
                .filter { it.matchesAny(classNames) }
                .flatMap { it.attributeConversions }
                .filter { it.matches(xmlAttributeName) }
                .flatMap { it.valueConversions }
        }
    }

    private val attributeInfosCache = hashMapOf<String, Map<String, List<AttributeInfo>>>()
    private fun getAttributeInfos(
        tagQualifiedName: String,
        ktxAttributeName: String
    ): List<AttributeInfo> {
        return attributeInfosCache.computeIfAbsent(tagQualifiedName) {
            val descriptor = findClass(tagQualifiedName)?.getJavaClassDescriptor()
                ?: return@computeIfAbsent emptyMap()
            val attributeInfos = arrayListOf<AttributeInfo>()
            // TODO(jdemeulenaere): Allow to filter with ktxAttributeName at the AttributeInfoExtractor level instead.
            attributeInfoExtractor.extract(descriptor) { attributeInfos.addAll(it) }
            attributeInfos.groupBy { it.name }
        }[ktxAttributeName] ?: emptyList()
    }

    private fun findClass(qualifiedName: String): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(qualifiedName, project.allScope())
    }

    private data class EnumValue(
        val attributeInfo: AttributeInfo,
        val fqNames: List<FqName>
    )

    private val enumValuesCache = hashMapOf<Pair<String, String>, List<EnumValue>>()
    private fun getEnumValues(tagQualifiedName: String, ktxAttributeName: String): List<EnumValue> {
        // TODO(jdemeulenaere): Precompute those for Android views or download android SDK & external annotations jars.
        // TODO(jdemeulenaere): We might want to somehow filter those, as some incorrect attribute conversions will be returned (e.g.
        // "notFocusable" in XML will be mapped to android.view.View.NOT_FOCUSABLE, which does not exist).
        return enumValuesCache.computeIfAbsent(Pair(tagQualifiedName, ktxAttributeName)) {
            getAttributeInfos(tagQualifiedName, ktxAttributeName).mapNotNull { attributeInfo ->
                attributeInfo.getPossibleValues(project)
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        EnumValue(
                            attributeInfo,
                            it
                        )
                    }
            }
        }
    }

    /**
     * Returns the common prefix length of the possible values, e.g: :
     *   getCommonPrefixLength(["TEXT_ALIGNMENT_TEXT_START", "TEXT_ALIGNMENT_CENTER"]) == "TEXT_ALIGNMENT_".length
     */
    private fun getCommonPrefixLength(possibleValues: List<String>): Int {
        // TODO(jdemeulenaere): In the latest android sources, the @IntDef attribute has a prefix value that gives exactly the prefix we
        // want to remove. Use that when generating the conversion configuration.

        var lastUnderscoreIndex = -1
        val minLength = possibleValues.map(String::length).min()!!
        for (i in 0 until minLength) {
            val char = possibleValues[0][i]
            if (!possibleValues.all { it[i] == char }) {
                break
            }
            if (char == '_') {
                lastUnderscoreIndex = i
            }
        }
        return lastUnderscoreIndex + 1
    }

    private fun convertTag(tag: XmlTag): Element {
        when (tag.name) {
            Companion.SpecialTags.MERGE.tagName -> return DeclarationStatement(tag.subTags.map {
                convertTag(it)
            })
            // TODO(jdemeulenaere): Instead of ignoring the <include> tag, we might want to suggest the user to convert the included layout
            // file into a component, or inline and convert its content.
            Companion.SpecialTags.INCLUDE.tagName,
            // TODO(jdemeulenaere): Make sure we want to ignore <tag> and <requestFocus/> tags.
            Companion.SpecialTags.TAG.tagName,
            Companion.SpecialTags.REQUEST_FOCUS.tagName -> return Element.Empty
        }

        val attributes = tag.attributes.mapNotNull(::maybeConvertAttribute)
        val body = tag.subTags.map(::convertTag)
        val (simpleName, qualifiedName) = getTagSimpleAndQualifiedName(tag)
        return KtxElement(simpleName.asIdentifier(qualifiedName), attributes, body)
    }

    private fun getTagSimpleAndQualifiedName(tag: XmlTag): Pair<String, String> {
        val fullName = when (tag.name) {
            Companion.SpecialTags.VIEW.tagName -> tag.getAttributeValue("class") ?: "android.view.View"
            // The <blink></blink> tag will be inflated as a android.view.LayoutInflater.BlinkLayout, which is a private class that extends
            // FrameLayout. User will lose the blinking but this is the closest we can get.
            Companion.SpecialTags.BLINK.tagName -> "android.widget.FrameLayout"
            else -> tag.name
        }

        val dotIndex = fullName.lastIndexOf('.')
        if (dotIndex != -1) {
            // TODO(jdemeulenaere): Check that import doesn't conflict with another (existing or future) import (in which case use the fully qualified name).
            return fullName.substring(dotIndex + 1) to fullName
        }

        val qualifiedName = CLASS_PREFIX_LIST
            .map { "$it$fullName" }
            .firstOrNull {
                JavaPsiFacade.getInstance(project).findClass(it, project.allScope()) != null
            } ?: "android.widget.$fullName"
        return fullName to qualifiedName
    }

    private fun XmlAttribute.isInNamespace(ns: XmlNamespace): Boolean {
        return if (namespace.isNotEmpty()) {
            namespace == ns.uri
        } else {
            namespacePrefix == ns.defaultPrefix
        }
    }

    private fun shouldConvertAttribute(attribute: XmlAttribute): Boolean {
        return attribute.namespacePrefix != NAMESPACE_PREFIX &&
                !attribute.isInNamespace(Companion.XmlNamespace.ANDROID_TOOLS) &&
                !(attribute.value ?: "").startsWith("@+id/") &&
                (attribute.parent.name != Companion.SpecialTags.VIEW.tagName || attribute.name != "class")
    }

    private fun maybeConvertAttribute(attribute: XmlAttribute): KtxAttribute? {
        if (!shouldConvertAttribute(attribute)) {
            return null
        }

        return convertAttribute(attribute)
    }

    private fun convertAttribute(attribute: XmlAttribute): KtxAttribute {
        val xmlName = attribute.localName // Strips away the namespace.
        val ktxName = getKtxAttributeName(xmlName)
        val xmlValue = attribute.value ?: ""

        val tag: XmlTag? = attribute.parent

        // Convert the value.
        val tagQualifiedName = tag?.let { getTagSimpleAndQualifiedName(it).second }
        val valueConversions = tagQualifiedName?.let {
            getValueConversions(it, xmlName)
        } ?: emptyList()
        val conversionResult = valueConversions
            .asSequence()
            .filter { it.matches(xmlValue) }
            .mapNotNull {
                it.convert(object : ConversionContext {
                    override val xmlName = xmlName
                    override val xmlValue = xmlValue
                })
            }
            .firstIsInstanceOrNull<ConversionResult.Success>()

        var valueType: KotlinType? = null
        var valueExpression: Expression? = null
        if (conversionResult != null) {
            // If conversion from the ConversionConfiguration succeeded, we end up with an Expression and the fully qualified name of this
            // expression type. We compare the fully qualified name with the KotlinType of the AttributeInfos corresponding to this attribute
            // to try to guess if we should import any extension function/property.
            // The current logic is that if there is one and only one AttributeInfo with that type, and that AttributeInfo is associated to
            // an extension, then we import it, otherwise we don't.

            // TODO(jdemeulenaere): Make sure that the comparison with f.q. name from converter and KotlinType from AttributeInfo works well
            // for all cases (e.g. generics).

            valueExpression = conversionResult.expression

            // TODO(jdemeulenaere/lelandr): There is certainly a better way to map FqName => KotlinType.
            val fqName = conversionResult.kotlinType
            val classDescriptor = DefaultBuiltIns.Instance.builtInsModule.resolveClassByFqName(
                fqName,
                NoLookupLocation.FROM_BUILTINS
            ) ?: findClass(fqName.asString())?.getJavaClassDescriptor()
            valueType = classDescriptor?.defaultType
        } else {
            // If conversion from ConversionConfiguration failed, then we try to match the XML value to an enum using reflection.

            val enumValues = tagQualifiedName?.let { getEnumValues(it, ktxName) }
            enumValues?.forEach { (attributeInfo, possibleValues) ->
                assert(possibleValues.isNotEmpty())

                // TODO(jdemeulenaere): We certainly want to remove common prefix only for values coming from magic annotations and not
                //  actual enum classes.
                val commonPrefixLength =
                    if (possibleValues.size == 1) 0 else getCommonPrefixLength(possibleValues.map {
                        it.shortName().asString()
                    })

                // We remove all non alpha numeric characters and lower case when matching values.
                fun simplify(s: String) = s.replace(NON_ALPHANUMERIC_PATTERN, "").toLowerCase()

                possibleValues.forEach { fqName ->
                    val shortName = fqName.shortName().asString()
                    val upperUnderscoreValue = shortName.substring(commonPrefixLength)

                    if (simplify(upperUnderscoreValue) == simplify(xmlValue)) {
                        val nameImport = attributeInfo.descriptor?.takeIf {
                            it.isExtension
                        }?.importableFqName
                        return KtxAttribute(
                            ktxName.asIdentifier(nameImport),
                            shortName.asIdentifier(fqName)
                        )
                    }
                }
            }
        }

        if (valueExpression == null) {
            valueExpression = LiteralExpression(xmlValue.quoted())
            valueType = DefaultBuiltIns.Instance.stringType
        }

        // Add import if we are using an extension function.
        val attributeImport = valueType?.let { theValueType ->
            val attributeInfos = tagQualifiedName?.let { getAttributeInfos(it, ktxName) }
                ?: emptyList()
            attributeInfos
                .filter { theValueType.isSubtypeOf(it.type) }
                // Only import extension function if it's the only one matching that KotlinType.
                .takeIf { it.size == 1 }
                ?.firstOrNull { it.isExtension }
                ?.let { it.descriptor?.importableFqName }
        }

        return KtxAttribute(ktxName.asIdentifier(attributeImport), valueExpression)
    }

    private fun getKtxAttributeName(
        xmlName: String
    ) = xmlName.replace(Regex("_([a-zA-Z\\d])")) { it.groupValues[1].toUpperCase() }

    private fun String.quoted() = "\"${escaped()}\""

    private fun String.escaped(): String {
        return this
            .replace("\\", "\\\\") // Replaces all \ by \\
            .replace("\"", "\\\"") // Replaces all " by \"
            .replace("\$", "\\\$") // Replaces all $ by \$
    }
}