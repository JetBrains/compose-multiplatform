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