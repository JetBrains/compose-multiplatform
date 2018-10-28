/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.r4a.R4aUtils
import java.util.regex.Pattern

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
        is XmlAttribute -> convertAttribute(element)
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

    private fun convertTag(tag: XmlTag): KtxElement {
        val (import, tagName) = getTagImportAndName(tag)
        val attributes = tag.attributes
            .filter(::shouldConvertAttribute)
            .map(::convertAttribute)

        val body = tag.subTags.map { convertTag(it) }
        return KtxElement(tagName.asIdentifier(import), attributes, body)
    }

    private fun getTagImportAndName(tag: XmlTag): Pair<FqName, String> {
        val fullName = tag.name
        val dotIndex = fullName.lastIndexOf('.')
        if (dotIndex != -1) {
            // TODO(jdemeulenaere): Check that import doesn't conflict with another (existing or future) import (in which case use the fully qualified name).
            return FqName(fullName) to fullName.substring(dotIndex + 1)
        }

        // TODO(jdemeulenaere): Not sure this default package is always correct.
        return FqName("android.widget.$fullName") to fullName
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

    private fun convertAttribute(attribute: XmlAttribute): KtxAttribute {
        // TODO(jdemeulenaere): Improve attribute mapping.
        val xmlName = attribute.localName // Strips away the namespace.
        val ktxName = xmlName.replace(Regex("_([a-zA-Z\\d])")) { it.groupValues[1].toUpperCase() }
        // TODO(jdemeulenaere): Better handling of missing value.
        val xmlValue = attribute.value ?: ""
        val ktxValue = convertAttributeValue(xmlName, xmlValue, attribute.isInNamespace(XmlNamespace.ANDROID))
        return KtxAttribute(ktxName, ktxValue)
    }

    private val RESOURCE_TYPES = listOf(
        "anim", "animator", "array", "attr", "bool", "color", "dimen", "drawable", "font", "id", "integer", "layout", "menu", "mipmap",
        "string", "style", "styleable"
    ).joinToString("|")
    private val RESOURCE_PATTERN = Pattern.compile("^@(([^:]+):)?($RESOURCE_TYPES)/(.+)$")
    private val DIMENSION_PATTERN = Pattern.compile("^(\\d+(\\.\\d+)?)(px|dip|dp|sp|pt|in|mm)$")
    private val viewEnumsMap = mapOf(
        "focusable" to mapOf(
            "auto" to "FOCUSABLE_AUTO",
            "true" to "FOCUSABLE",
            "false" to "NOT_FOCUSABLE"
        ),
        "visibility" to mapOf(
            "visible" to "VISIBLE",
            "invisible" to "INVISIBLE",
            "gone" to "GONE"
        ),
        "importantForAutofill" to mapOf(
            "auto" to "IMPORTANT_FOR_AUTOFILL_AUTO",
            "yes" to "IMPORTANT_FOR_AUTOFILL_YES",
            "no" to "IMPORTANT_FOR_AUTOFILL_NO",
            "yesExcludeDescendants" to "IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS",
            "noExcludeDescendants" to "IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS"
        ),
        "drawingCacheQuality" to mapOf(
            "auto" to "DRAWING_CACHE_QUALITY_AUTO",
            "high" to "DRAWING_CACHE_QUALITY_HIGH",
            "low" to "DRAWING_CACHE_QUALITY_LOW"
        ),
        "scrollbarStyle" to mapOf(
            "insideOverlay" to "SCROLLBARS_INSIDE_OVERLAY",
            "insideInset" to "SCROLLBARS_INSIDE_INSET",
            "outsideOverlay" to "SCROLLBARS_OUTSIDE_OVERLAY",
            "outsideInset" to "SCROLLBARS_OUTSIDE_INSET"
        ),
        "layoutDirection" to mapOf(
            "ltr" to "LAYOUT_DIRECTION_LTR",
            "rtl" to "LAYOUT_DIRECTION_RTL",
            "inherit" to "LAYOUT_DIRECTION_INHERIT",
            "locale" to "LAYOUT_DIRECTION_LOCALE"
        ),
        "textAlignment" to mapOf(
            "inherit" to "TEXT_ALIGNMENT_INHERIT",
            "gravity" to "TEXT_ALIGNMENT_GRAVITY",
            "center" to "TEXT_ALIGNMENT_CENTER",
            "textStart" to "TEXT_ALIGNMENT_TEXT_START",
            "textEnd" to "TEXT_ALIGNMENT_TEXT_END",
            "viewStart" to "TEXT_ALIGNMENT_VIEW_START",
            "viewEnd" to "TEXT_ALIGNMENT_VIEW_END"
        ),
        "scrollIndicators" to mapOf(
            "top" to "SCROLL_INDICATOR_TOP",
            "bottom" to "SCROLL_INDICATOR_BOTTOM",
            "left" to "SCROLL_INDICATOR_LEFT",
            "right" to "SCROLL_INDICATOR_RIGHT",
            "start" to "SCROLL_INDICATOR_START",
            "end" to "SCROLL_INDICATOR_END"
        )
    )

    private fun convertAttributeValue(name: String, value: String, inAndroidNamespace: Boolean): Expression {
        // TODO(jdemeulenaere): Improve attribute value conversion. Current implementation simply apply some pattern matching logic on the
        // value to guess what the attribute value should be converted to.
        // A second better solution should reflect on available setters/properties on the tag class, and apply the current pattern matching
        // logic to select between multiple candidates. I am not sure what's the proper way to do that, but
        // PsiShortNamesCache::getFieldsByName and PsiShortNamesCache::getMethodsByName might be helpful.
        // A third solution would be to simply keep a mapping (TagClass, AttributeName) => ConversionType of the core android widgets.

        if (value == "true" || value == "false") {
            return LiteralExpression(value)
        }

        // TODO(jdemeulenaere): Instead of enumerating all possible enum values, reflect on value type and do it automatically for enums (in
        // general, int parameters meta-annotated by @IntDef). Until that better solution, I only handle View enums explictly.
        if (inAndroidNamespace) {
            val enumValue = viewEnumsMap[name]?.get(value)
            if (enumValue != null) {
                return enumValue.asIdentifier(FqName("android.view.View.$enumValue"))
            }

            if (name == "layout_width" || name == "layout_height") {
                when (value) {
                    "match_parent", "fill_parent" -> return "MATCH_PARENT".asIdentifier(FqName("android.view.ViewGroup.LayoutParams.MATCH_PARENT"))
                    "wrap_content" -> return "WRAP_CONTENT".asIdentifier(FqName("android.view.ViewGroup.LayoutParams.WRAP_CONTENT"))
                }
            }
        }

        // Resource.
        // TODO(jdemeulenaere): Also handle style attribute: ?[<package_name>:][<resource_type>/]<resource_name>
        val resourceMatcher = RESOURCE_PATTERN.matcher(value)
        if (resourceMatcher.matches()) {
            return with(resourceMatcher) { resourceExpression(group(2), group(3), group(4)) }
        }

        // Dimension.
        val dimensionMatcher = DIMENSION_PATTERN.matcher(value)
        if (dimensionMatcher.matches()) {
            return dimensionExpression(LiteralExpression(dimensionMatcher.group(1)), dimensionMatcher.group(3))
        }

        // Return as a simple string.
        return LiteralExpression(value.quoted())
    }

    // Returns expression: [<package>.]R.<type>.<name>
    private fun resourceExpression(packageName: String?, type: String, name: String): Expression {
        // TODO(jdemeulenaere): We directly return the resource ID
        return if (packageName == null) {
            resourceExpression(type, name)
        } else {
            QualifiedExpression(
                qualifier = packageName.asIdentifier(),
                identifier = resourceExpression(type, name),
                dotPrototype = null
            )
        }
    }

    // Returns expression: R.<type>.<name>
    private fun resourceExpression(type: String, name: String): Expression {
        return QualifiedExpression(
            qualifier = QualifiedExpression(
                qualifier = "R".asIdentifier(),
                identifier = type.asIdentifier(),
                dotPrototype = null
            ),
            identifier = name.asIdentifier(),
            dotPrototype = null
        )
    }

    private fun String.asIdentifier(import: FqName? = null) = Identifier(this, isNullable = false, imports = import?.let(::listOf) ?: emptyList())

    // Returns expression: <value>.<unit>
    private fun dimensionExpression(value: Expression, unit: String): Expression {
        return QualifiedExpression(
            qualifier = value,
            identifier = unit.asIdentifier(R4aUtils.r4aFqName("adapters.$unit")),
            dotPrototype = null
        )
    }

    private fun String.quoted() = "\"${escaped()}\""

    private fun String.escaped(): String {
        return this
            .replace("\\", "\\\\") // Replaces all \ by \\
            .replace("\"", "\\\"") // Replaces all " by \"
            .replace("\$", "\\\$") // Replaces all $ by \$
    }

}