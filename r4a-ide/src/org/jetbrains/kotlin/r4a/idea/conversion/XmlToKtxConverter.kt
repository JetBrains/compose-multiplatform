/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

object XmlToKtxConverter {
    const val NAMESPACE_PREFIX = "xmlns"

    enum class XmlNamespace(val uri: String, val defaultPrefix: String) {
        ANDROID("http://schemas.android.com/apk/res/android", "android"),
        ANDROID_TOOLS("http://schemas.android.com/tools", "tools"),
        ANDROID_APP("http://schemas.android.com/apk/res-auto", "app");
    }

    fun convertElement(
        element: PsiElement
    ): Element = when (element) {
        is XmlFile -> convertFile(element)
        is XmlTag -> convertTag(element)
        // TODO(jdemeulenaere): If no parent tag, look if we are pasting in existing KTX tag.
        is XmlAttribute -> convertAttribute(element, element.parent?.let { getAttributeConversions(it) } ?: emptyList())
        else -> DummyStringExpression(element.text)
    }

    fun convertFile(element: XmlFile): Element {
        return element.rootTag?.let { convertTag(it) } ?: DummyStringExpression("")
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

    private fun getAttributeConversions(tag: XmlTag): List<AttributeConversion> {
        val (simpleName, qualifiedName) = getTagSimpleAndQualifiedName(tag)
        val project = tag.project
        val descriptor = PsiShortNamesCache.getInstance(project)
            .getClassesByName(simpleName, project.allScope())
            .firstOrNull { it.qualifiedName == qualifiedName }?.getJavaClassDescriptor() ?: return emptyList()
        val classNames = descriptor.getAllSuperClassifiers().toList().mapNotNull { it.fqNameOrNull()?.asString() }.toSet()
        return ANDROID_CONVERSION.classConversions
            .filter { it.matchesAny(classNames) }
            .flatMap { it.attributeConversions }
    }

    private fun convertTag(tag: XmlTag): KtxElement {
        val attributeConversions = getAttributeConversions(tag)
        val attributes = tag.attributes
            .filter(::shouldConvertAttribute)
            .map { convertAttribute(it, attributeConversions) }

        val body = tag.subTags.map { convertTag(it) }
        val (simpleName, qualifiedName) = getTagSimpleAndQualifiedName(tag)
        return KtxElement(simpleName.asIdentifier(FqName(qualifiedName)), attributes, body)
    }

    private fun getTagSimpleAndQualifiedName(tag: XmlTag): Pair<String, String> {
        val fullName = tag.name
        val dotIndex = fullName.lastIndexOf('.')
        if (dotIndex != -1) {
            // TODO(jdemeulenaere): Check that import doesn't conflict with another (existing or future) import (in which case use the fully qualified name).
            return fullName.substring(dotIndex + 1) to fullName
        }

        // TODO(jdemeulenaere): Not sure this default package is always correct.
        return fullName to "android.widget.$fullName"
    }

    private fun XmlAttribute.isInNamespace(ns: XmlNamespace): Boolean {
        return if (namespace.isNotEmpty()) {
            namespace == ns.uri
        } else {
            namespacePrefix == ns.defaultPrefix
        }
    }

    private fun shouldConvertAttribute(attribute: XmlAttribute): Boolean {
        return attribute.namespacePrefix != NAMESPACE_PREFIX && !attribute.isInNamespace(XmlNamespace.ANDROID_TOOLS)
        && !(attribute.value ?: "").startsWith("@+id/")
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