// Defines shared package, type, member, and suppression names used by the generator.
package prototype.dom.generator

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.UNIT

// Browser packages.
internal const val DOM_PACKAGE = "org.w3c.dom"
internal const val DOM_CSS_PACKAGE = "org.w3c.dom.css"
internal const val DOM_EVENTS_PACKAGE = "org.w3c.dom.events"
internal const val WEBGL_PACKAGE = "org.khronos.webgl"

// Facade packages.
internal const val PORTABLE_JS_PACKAGE = "kotlinx.browser"
internal const val PORTABLE_DOM_PACKAGE = "kotlinx.browser.dom"
internal const val PORTABLE_CSS_PACKAGE = "kotlinx.browser.dom.css"
internal const val PORTABLE_EVENTS_PACKAGE = "kotlinx.browser.dom.events"
internal const val PORTABLE_WEBGL_PACKAGE = "kotlinx.browser.webgl"

internal data class PortablePackageMapping(
    val portablePackage: String,
    val declarationsFile: String,
    val dictionariesFile: String,
) {
    val interopFile = "${declarationsFile}Interop"
    val valuesFile = "EnumLikeValues"
}

internal val PORTABLE_PACKAGE_BY_BROWSER_PACKAGE = linkedMapOf(
    DOM_PACKAGE to PortablePackageMapping(PORTABLE_DOM_PACKAGE, "PortableDom", "OptionDictionaries"),
    DOM_CSS_PACKAGE to PortablePackageMapping(PORTABLE_CSS_PACKAGE, "PortableCss", "CssDictionaries"),
    DOM_EVENTS_PACKAGE to PortablePackageMapping(PORTABLE_EVENTS_PACKAGE, "PortableEvents", "EventDictionaries"),
)

// Explicit mappings for browser packages outside the org.w3c naming convention.
internal val EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE = linkedMapOf(
    WEBGL_PACKAGE to PortablePackageMapping(PORTABLE_WEBGL_PACKAGE, "PortableTypedArrays", "TypedArrayDictionaries"),
)

internal fun facadePackageMappings(signatureOnlyPackages: Set<String>): Map<String, PortablePackageMapping> =
    PORTABLE_PACKAGE_BY_BROWSER_PACKAGE +
        EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE +
        signatureOnlyPackages
            .filterNot(EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE::containsKey)
            .associateWith(::signatureOnlyPackageMapping)

/** Converts a policy-selected browser package without deciding whether it is selected. */
internal fun signatureOnlyPackageMapping(browserPackage: String) = PortablePackageMapping(
    browserPackage.replaceFirst("org.w3c", "kotlinx.browser"),
    "SignatureTypes",
    "SignatureDictionaries",
)

internal const val STAGING_ROOT = "portableDom"
internal const val PORTABLE_INTEROP_FILE = "PortableInterop"

// Interop types require different actuals on JS, Wasm/JS, and JVM.
internal val PORTABLE_JS_ANY = ClassName(PORTABLE_JS_PACKAGE, "JsAny")
internal val PORTABLE_JS_STRING = ClassName(PORTABLE_JS_PACKAGE, "JsString")
internal val PORTABLE_JS_NUMBER = ClassName(PORTABLE_JS_PACKAGE, "JsNumber")
internal val PORTABLE_JS_DOUBLE = ClassName(PORTABLE_JS_PACKAGE, "JsDouble")
internal val PORTABLE_JS_ARRAY = ClassName(PORTABLE_JS_PACKAGE, "JsArray")
internal val PORTABLE_PROMISE = ClassName(PORTABLE_JS_PACKAGE, "Promise")

internal val BROWSER_JS_ANY = ClassName("kotlin.js", "JsAny")
internal val BROWSER_JS_STRING = ClassName("kotlin.js", "JsString")
internal val BROWSER_JS_NUMBER = ClassName("kotlin.js", "JsNumber")
internal val BROWSER_JS_ARRAY = ClassName("kotlin.js", "JsArray")
internal val BROWSER_PROMISE = ClassName("kotlin.js", "Promise")

internal val PORTABLE_INTEROP_TYPES: Map<String, ClassName> = listOf(
    BROWSER_JS_ANY to PORTABLE_JS_ANY,
    BROWSER_JS_STRING to PORTABLE_JS_STRING,
    BROWSER_JS_NUMBER to PORTABLE_JS_NUMBER,
    BROWSER_JS_ARRAY to PORTABLE_JS_ARRAY,
    BROWSER_PROMISE to PORTABLE_PROMISE,
).associate { (browser, portable) -> browser.canonicalName to portable }

internal val TO_JS_STRING = MemberName(PORTABLE_JS_PACKAGE, "toJsString")
internal val TO_JS_NUMBER = MemberName(PORTABLE_JS_PACKAGE, "toJsNumber")
internal val TO_JS_DOUBLE = MemberName(PORTABLE_JS_PACKAGE, "toJsDouble")
internal val TO_JS_ARRAY = MemberName(PORTABLE_JS_PACKAGE, "toJsArray")
internal val EMPTY_JS_ANY = ClassName(PORTABLE_JS_PACKAGE, "EmptyJsAny")

internal fun ClassName.browserImportAlias(): String = "Browser$simpleName"

internal fun ClassName.companionName(): ClassName = nestedClass("Companion")

// Common code cannot reference kotlin.js.definedExternally.
internal val DEFINED_EXTERNALLY = MemberName(PORTABLE_DOM_PACKAGE, "definedExternally")

internal val BUILTIN_TYPES: Map<String, ClassName> =
    listOf(ANY, BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, STRING, UNIT)
        .associateBy(ClassName::canonicalName)

internal val IGNORED_SUPERTYPES = setOf("kotlin.Any", BROWSER_JS_ANY.canonicalName)

internal const val DEFAULT_ARGUMENTS_SUPPRESSION = "DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS"

internal const val CLASS_SCOPE_SUPPRESSION = "EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE"

internal const val JVM_DEFAULT_ARGUMENTS_SUPPRESSION = "ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS"

// JVM mixin storage and the web JsNumber aliases differ in modality.
internal const val MODALITY_SUPPRESSION = "EXPECT_ACTUAL_INCOMPATIBLE_MODALITY"

internal const val IR_SUPPRESSION = "EXPECT_ACTUAL_IR_INCOMPATIBILITY"

internal const val VARIANCE_SUPPRESSION = "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE"
