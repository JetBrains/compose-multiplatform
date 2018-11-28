/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.r4a.idea.editor.KtxEditorOptions
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractXmlToKtxCopyPasteConversionTest : BaseXmlToKtxConversionTest() {
    private val DEFAULT_COPIED_XML = "DefaultCopiedXml.xml"

    private var oldEditorOptions: KtxEditorOptions? = null

    override fun setUp() {
        super.setUp()
        oldEditorOptions = KtxEditorOptions.getInstance().state
        KtxEditorOptions.getInstance().enableXmlToKtxConversion = true
        KtxEditorOptions.getInstance().enableAddComposableAnnotation = true
        KtxEditorOptions.getInstance().donTShowKtxConversionDialog = true
        KtxEditorOptions.getInstance().donTShowAddComposableAnnotationDialog = true
    }

    override fun tearDown() {
        oldEditorOptions?.let { KtxEditorOptions.getInstance().loadState(it) }
        super.tearDown()
    }

    fun doTest(path: String) {
        val testName = getTestName(false)
        val xmlFileName = "$testName.xml"
        if (File(testDataPath + File.separator + xmlFileName).exists()) {
            myFixture.configureByFile(xmlFileName)
        } else {
            myFixture.configureByFile(DEFAULT_COPIED_XML)
        }
        myFixture.performEditorAction(IdeActions.ACTION_COPY)
        myFixture.configureByFile("$testName.to.kt")
        myFixture.performEditorAction(IdeActions.ACTION_PASTE)
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".to.kt", ".expected.kt")), myFixture.file.text)
    }
}