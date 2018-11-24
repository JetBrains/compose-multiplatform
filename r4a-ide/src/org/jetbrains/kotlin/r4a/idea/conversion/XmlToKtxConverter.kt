/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.google.common.base.CaseFormat
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.core.isExcludedFromAutoImport
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.r4a.idea.AttributeInfoExtractor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import java.util.*

class XmlToKtxConverter(private val targetFile: KtFile) {
    companion object {
        private const val NAMESPACE_PREFIX = "xmlns"
        private val MAGIC_CONSTANT_ANNOTATIONS = listOf("IntDef", "LongDef", "StringDef").flatMap {
            listOf("android.annotation.$it", "android.support.annotation.$it")
        }

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
    // TODO(jdemeulenaere): Compare with `isVisibleDescriptor` in (BaseR4a)CompletionSession to add more checks here.
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean = lambda@{ descriptor ->
        if (descriptor is DeclarationDescriptorWithVisibility) {
            return@lambda descriptor.isVisible(targetFile.findModuleDescriptor())
        }

        return@lambda descriptor.isExcludedFromAutoImport(project, targetFile)
    }
    private val attributeInfoExtractor = AttributeInfoExtractor(targetFile, visibilityFilter)
    private val classToAttributeConversions = hashMapOf<String, List<AttributeConversion>>()

    fun convertElement(element: PsiElement): Element = when (element) {
        is XmlFile -> convertFile(element)
        is XmlTag -> convertTag(element)
        // TODO(jdemeulenaere): If no parent tag, look if we are pasting in existing KTX tag.
        is XmlAttribute -> convertAttribute(element, element.parent?.let { getAttributeConversions(it) } ?: emptyList())
        else -> DummyStringExpression(element.text)
    }

    private fun convertFile(element: XmlFile): Element {
        return element.rootTag?.let { convertTag(it) } ?: Element.Empty
    }

    private fun getAttributeConversions(tag: XmlTag): List<AttributeConversion> {
        val (_, qualifiedName) = getTagSimpleAndQualifiedName(tag)
        val cachedAttributeConversions = classToAttributeConversions[qualifiedName]
        if (cachedAttributeConversions != null) {
            return cachedAttributeConversions
        }

        val attributeConversions = doGetAttributeConversions(qualifiedName)
        classToAttributeConversions[qualifiedName] = attributeConversions
        return attributeConversions
    }

    private fun doGetAttributeConversions(qualifiedName: String): List<AttributeConversion> {
        val descriptor = findClass(qualifiedName)?.getJavaClassDescriptor() ?: return emptyList()
        val classNames = descriptor.getAllSuperClassifiers().toList().mapNotNull { it.fqNameOrNull()?.asString() }.toSet()
        return ANDROID_CONVERSION.classConversions
            .filter { it.matchesAny(classNames) }
            .flatMap { it.attributeConversions }
            .plus(getEnumAttributeConversions(descriptor))
    }

    private fun findClass(qualifiedName: String): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(qualifiedName, project.allScope())
    }

    private fun getEnumAttributeConversions(descriptor: ClassDescriptor): List<AttributeConversion> {
        // TODO(jdemeulenaere): Precompute those for Android views or download android SDK & external annotations jars.
        // TODO(jdemeulenaere): Handle normal Java/Kotlin enums.
        // TODO(jdemeulenaere): We might want to somehow filter those, as some incorrect attribute conversions will be returned (e.g.
        // "notFocusable" in XML will be mapped to android.view.View.NOT_FOCUSABLE).
        val enumConversions = arrayListOf<AttributeConversion>()
        attributeInfoExtractor.extract(descriptor) {
            it
                .forEach { attributeInfo ->
                    // TODO(jdemeulenaere): Handle annotated properties values.
                    val psiMethod = attributeInfo.descriptor?.findPsi() as? PsiMethod ?: return@forEach

                    // This should not happen.
                    if (psiMethod.parameterList.parametersCount != 1) {
                        return@forEach
                    }

                    val psiParameter = psiMethod.parameters.first() as PsiParameter
                    val possibleValues = getPossibleEnumValues(psiParameter)
                    if (possibleValues.isEmpty()) {
                        return@forEach
                    }

                    val commonPrefixLength = if (possibleValues.size == 1) 0 else getCommonPrefixLength(possibleValues.map { it.shortName().asString() })

                    val attributeConversion = ExactAttributeConversion(attributeInfo.name)
                    attributeConversion.valueConversions.addAll(possibleValues.map { fqName ->
                        val shortName = fqName.shortName()
                        val upperUnderscoreValue = shortName.asString().substring(commonPrefixLength)
                        val xmlValue = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, upperUnderscoreValue)
                        ExactValueConversion(xmlValue) { Identifier(shortName.asString(), imports = listOf(fqName)) }
                    })
                    enumConversions.add(attributeConversion)
                }
        }
        return enumConversions
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

    private fun getPossibleEnumValues(annotated: PsiModifierListOwner): List<FqName> {
        val enumFqNames = arrayListOf<FqName>()
        fillPossibleEnumValues(annotated, enumFqNames, hashSetOf())
        return enumFqNames
    }

    private fun fillPossibleEnumValues(
        annotated: PsiModifierListOwner,
        enumFqNames: ArrayList<FqName>,
        visited: MutableSet<PsiClass>
    ) {
        val annotations = AnnotationUtil.getAllAnnotations(annotated, /* inHierarchy= */ true, /* visited= */ null)
        for (annotation in annotations) {
            val qualifiedName = annotation.qualifiedName ?: continue
            if (qualifiedName in MAGIC_CONSTANT_ANNOTATIONS) {
                enumFqNames.addAll(getMagicConstantEnumValues(annotation))
            } else {
                val annotationClass = findClass(qualifiedName) ?: continue
                if (!visited.add(annotationClass)) continue
                fillPossibleEnumValues(annotationClass, enumFqNames, visited)
            }
        }
    }

    private fun getMagicConstantEnumValues(annotation: PsiAnnotation): List<FqName> {
        val valueAttribute = annotation.findAttributeValue("value")
        val allowedValues = (valueAttribute as? PsiArrayInitializerMemberValue)?.initializers ?: PsiAnnotationMemberValue.EMPTY_ARRAY
        return allowedValues
            .filterIsInstance<PsiReference>()
            .map(PsiReference::resolve)
            .filterIsInstance<PsiNamedElement>()
            .mapNotNull { it.getKotlinFqName() }
    }

    private fun convertTag(tag: XmlTag): Element {
        when (tag.name) {
            SpecialTags.MERGE.tagName -> return DeclarationStatement(tag.subTags.map { convertTag(it) })
            // TODO(jdemeulenaere): Instead of ignoring the <include> tag, we might want to suggest the user to convert the included layout
            // file into a component, or inline and convert its content.
            SpecialTags.INCLUDE.tagName,
            // TODO(jdemeulenaere): Make sure we want to ignore <tag> and <requestFocus/> tags.
            SpecialTags.TAG.tagName,
            SpecialTags.REQUEST_FOCUS.tagName -> return Element.Empty
        }

        val attributeConversions = getAttributeConversions(tag)
        val attributes = tag.attributes
            .filter { shouldConvertAttribute(tag, it) }
            .map { convertAttribute(it, attributeConversions) }

        val body = tag.subTags.map { convertTag(it) }
        val (simpleName, qualifiedName) = getTagSimpleAndQualifiedName(tag)
        return KtxElement(simpleName.asIdentifier(FqName(qualifiedName)), attributes, body)
    }

    private fun getTagSimpleAndQualifiedName(tag: XmlTag): Pair<String, String> {
        val fullName = when(tag.name) {
            SpecialTags.VIEW.tagName -> tag.getAttributeValue("class") ?: "android.view.View"
            // The <blink></blink> tag will be inflated as a android.view.LayoutInflater.BlinkLayout, which is a private class that extends
            // FrameLayout. User will lose the blinking but this is the closest we can get.
            SpecialTags.BLINK.tagName -> "android.widget.FrameLayout"
            else -> tag.name
        }

        val dotIndex = fullName.lastIndexOf('.')
        if (dotIndex != -1) {
            // TODO(jdemeulenaere): Check that import doesn't conflict with another (existing or future) import (in which case use the fully qualified name).
            return fullName.substring(dotIndex + 1) to fullName
        }

        val qualifiedName = CLASS_PREFIX_LIST
            .map { "$it$fullName" }
            .firstOrNull { JavaPsiFacade.getInstance(project).findClass(it, project.allScope()) != null }
            ?: "android.widget.$fullName"
        return fullName to qualifiedName
    }

    private fun XmlAttribute.isInNamespace(ns: XmlNamespace): Boolean {
        return if (namespace.isNotEmpty()) {
            namespace == ns.uri
        } else {
            namespacePrefix == ns.defaultPrefix
        }
    }

    private fun shouldConvertAttribute(tag: XmlTag, attribute: XmlAttribute): Boolean {
        return attribute.namespacePrefix != NAMESPACE_PREFIX && !attribute.isInNamespace(XmlNamespace.ANDROID_TOOLS)
                && !(attribute.value ?: "").startsWith("@+id/") && (tag.name != SpecialTags.VIEW.tagName || attribute.name != "class")
    }

    private fun convertAttribute(attribute: XmlAttribute, attributeConversions: List<AttributeConversion>): KtxAttribute {
        // TODO(jdemeulenaere): Improve attribute mapping.
        val xmlName = attribute.localName // Strips away the namespace.
        val ktxName = xmlName.replace(Regex("_([a-zA-Z\\d])")) { it.groupValues[1].toUpperCase() }
        // TODO(jdemeulenaere): Better handling of missing value.
        val xmlValue = attribute.value ?: ""
        val ktxValue = convertAttributeValue(xmlName, xmlValue, attributeConversions)
        return KtxAttribute(ktxName, ktxValue)
    }

    private fun convertAttributeValue(name: String, value: String, attributeConversions: List<AttributeConversion>): Expression {
        // TODO(jdemeulenaere): Improve attribute value conversion. Current implementation simply apply some pattern matching logic on the
        // value to guess what the attribute value should be converted to.
        // A second better solution should reflect on available setters/properties on the tag class, and apply the current pattern matching
        // logic to select between multiple candidates. I am not sure what's the proper way to do that, but
        // PsiShortNamesCache::getFieldsByName and PsiShortNamesCache::getMethodsByName might be helpful.
        // A third solution would be to simply keep a mapping (TagClass, AttributeName) => ConversionType of the core android widgets.

        // Try all matching conversions.
        return attributeConversions
            .asSequence()
            .filter { it.matches(name) }
            .flatMap { it.valueConversions.asSequence() }
            .filter { it.matches(value) }
            .mapNotNull { it.convert(name, value) }
            .firstOrNull() ?: LiteralExpression(value.quoted())
    }

    private fun String.quoted() = "\"${escaped()}\""

    private fun String.escaped(): String {
        return this
            .replace("\\", "\\\\") // Replaces all \ by \\
            .replace("\"", "\\\"") // Replaces all " by \"
            .replace("\$", "\\\$") // Replaces all $ by \$
    }

}