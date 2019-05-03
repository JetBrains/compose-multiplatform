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