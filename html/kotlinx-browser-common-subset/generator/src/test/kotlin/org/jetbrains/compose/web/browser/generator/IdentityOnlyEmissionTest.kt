/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies identity-only declarations and hierarchy edges on every target.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ClassName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentityOnlyEmissionTest {
    /** External naming rules remain distinct from fully ported package mappings. */
    @Test
    fun thePackageCategoryIsSeparateFromFullyPortedPackages() {
        val packages = setOf("org.w3c.files", WEBGL_PACKAGE)
        val mappings = facadePackageMappings(packages)

        assertTrue("org.khronos.webgl.ArrayBuffer".browserPackage() in EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE)
        assertTrue("org.khronos.webgl.ArrayBuffer".browserPackage() in packages)
        assertTrue("org.w3c.files.Blob".browserPackage() in packages)
        assertFalse("org.khronos.webgl.ArrayBuffer".browserPackage() in COMMON_PACKAGE_BY_BROWSER_PACKAGE)
        assertFalse("org.w3c.files.Blob".browserPackage() in COMMON_PACKAGE_BY_BROWSER_PACKAGE)
        assertFalse("org.w3c.dom.Node".browserPackage() in EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE)
        assertEquals(COMMON_WEBGL_PACKAGE, mappings.getValue(WEBGL_PACKAGE).commonPackage)
        assertTrue("org.w3c.files" in mappings)
        assertFalse("org.w3c.xhr" in mappings)
    }

    /** The common declaration is the classifier and its subtype edge, and nothing else. */
    @Test
    fun commonDeclarationsAreBareIdentities() {
        val common = commonDeclarationsFile(WEBGL, listOf(ARRAY_BUFFER, VIEW, FLOAT_32), emptyList()).toString()

        assertContains(common, "public expect open class ArrayBuffer : JsAny")
        assertContains(common, "public expect interface ArrayBufferView : JsAny")
        assertContains(common, "public expect open class Float32Array : ArrayBufferView, JsAny")
        // No body anywhere: an identity-only classifier carries no member to put in one.
        assertFalse("{" in common)
    }

    /** Leaves browser constructors available only through web typealiases. */
    @Test
    fun noBrowserConstructorIsRepeated() {
        assertTrue(ARRAY_BUFFER.constructors.isEmpty())
        assertFalse("constructor" in commonDeclarationsFile(WEBGL, listOf(ARRAY_BUFFER), emptyList()).toString())
    }

    /** Web keeps browser type identity, so a facade value is the browser's own. */
    @Test
    fun webActualsAliasTheBrowserDeclaration() {
        val web = browserLeafDeclarationsFile(WEBGL, listOf(ARRAY_BUFFER, VIEW, FLOAT_32), emptyList()).toString()

        assertContains(web, "import org.khronos.webgl.ArrayBuffer")
        assertContains(web, "public actual typealias ArrayBuffer = ArrayBuffer")
        assertContains(web, "public actual typealias Float32Array = Float32Array")
        // Nothing here has members or constructors, so none of the expect/actual suppressions apply.
        assertFalse("Suppress" in web)
    }

    /** On the JVM there is no browser type to point at, so the identity is an empty stub. */
    @Test
    fun jvmActualsAreEmptyStubs() {
        val declarations = listOf(ARRAY_BUFFER, VIEW, FLOAT_32)
        val jvm = jvmDeclarationsFile(
            WEBGL,
            declarations,
            emptyList(),
            JvmStubValues(declarations.associateBy(CommonClass::commonName)),
            JvmConstantValues(emptyList()),
        ).toString()

        assertContains(jvm, "public actual open class ArrayBuffer : JsAny")
        assertContains(jvm, "public actual interface ArrayBufferView : JsAny")
        assertContains(jvm, "public actual open class Float32Array : ArrayBufferView, JsAny")
        assertFalse("{" in jvm)
    }

    /** Identity-only JVM classes retain their synthesized no-argument constructor. */
    @Test
    fun anIdentityOnlyClassifierHasAnInertValue() {
        val classes = listOf(ARRAY_BUFFER, VIEW, FLOAT_32).associateBy(CommonClass::commonName)

        assertEquals(
            "kotlinx.browser.webgl.Float32Array()",
            JvmStubValues(classes).value(FLOAT_32.commonName).toString(),
        )
    }

    /** Listed class parents survive just like listed interface edges; unlisted parents do not. */
    @Test
    fun aListedClassParentCanBeKeptWithoutAddingMembers() {
        val parent = identityOnly("TypedArrayBase", ClassShape.OPEN)
        val child = identityOnly("ConcreteTypedArray", ClassShape.OPEN, parent = parent)
        val declarations = listOf(parent, child)

        val common = commonDeclarationsFile(WEBGL, declarations, emptyList()).toString()
        val jvm = jvmDeclarationsFile(
            WEBGL,
            declarations,
            emptyList(),
            JvmStubValues(declarations.associateBy(CommonClass::commonName)),
            JvmConstantValues(emptyList()),
        ).toString()

        assertContains(common, "public expect open class ConcreteTypedArray : TypedArrayBase, JsAny")
        assertContains(jvm, "public actual open class ConcreteTypedArray : TypedArrayBase(), JsAny")
    }
}

private val WEBGL = CommonPackageMapping(COMMON_WEBGL_PACKAGE, "TypedArrays", "TypedArrayDictionaries")

private fun identityOnly(
    name: String,
    shape: ClassShape,
    parent: CommonClass? = null,
    superinterfaces: List<CommonClass> = emptyList(),
): CommonClass = CommonClass(
    browserName = ClassName(WEBGL_PACKAGE, name),
    parentBrowserName = parent?.browserName,
    superinterfaces = superinterfaces.map(CommonClass::commonName),
    ancestors = listOfNotNull(parent?.commonName),
    shape = shape,
    isDictionary = false,
    isJsAny = true,
    properties = emptyList(),
    functions = emptyList(),
    constructors = emptyList(),
    companion = null,
    factory = null,
    sourceFile = null,
)

private val ARRAY_BUFFER = identityOnly("ArrayBuffer", ClassShape.OPEN)
private val VIEW = identityOnly("ArrayBufferView", ClassShape.INTERFACE)
private val FLOAT_32 = identityOnly("Float32Array", ClassShape.OPEN, superinterfaces = listOf(VIEW))
