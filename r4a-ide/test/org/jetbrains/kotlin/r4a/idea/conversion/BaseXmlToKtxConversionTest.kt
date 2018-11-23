/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.AnnotationOrderRootType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class BaseXmlToKtxConversionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : KotlinJdkAndLibraryProjectDescriptor(emptyList()) {
            override fun configureModule(module: Module, model: ModifiableRootModel) {
                // We make use of external annotations (https://www.jetbrains.com/help/idea/external-annotations.html) to read annotations
                // that have a SOURCE retention.
                // The way we compute the JAR path is inspired from com.android.tools.idea.startup.ExternalAnnotationsSupport.
                val homePath = FileUtil.toSystemIndependentName(PathManager.getHomePath())
                val androidExternalAnnotationsJar = "$homePath/plugins/android/lib/androidAnnotations.jar"

                val filesToOrderRootType = mapOf(
                    File("plugins/r4a/r4a-runtime/build/libs/r4a-runtime-1.3-SNAPSHOT.jar") to OrderRootType.CLASSES,
                    KotlinTestUtils.findAndroidApiJar() to OrderRootType.CLASSES,
                    File(androidExternalAnnotationsJar) to AnnotationOrderRootType.getInstance()
                )

                val editor = NewLibraryEditor()
                editor.name = LIBRARY_NAME
                filesToOrderRootType.forEach { file, orderRootType ->
                    assert(file.exists()) { "File doesn't exist: " + file.absolutePath }
                    editor.addRoot(VfsUtil.getUrlForLibraryRoot(file), orderRootType)
                }
                ConfigLibraryUtil.addLibrary(editor, model)
            }
        }
    }
}