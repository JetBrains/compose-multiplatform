/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.preview.runtime

import kotlin.reflect.KProperty1

class ComposePreviewRunner {
    companion object {
        private const val PREVIEW_ANNOTATION_FQ_NAME = "androidx.compose.ui.tooling.desktop.preview.Preview"

        @JvmStatic
        fun main(args: Array<String>) {
            val classLoader = ComposePreviewRunner::class.java.classLoader

            val previewFqName = args[0]
            val previewClassFqName = previewFqName.substringBeforeLast(".")
            val previewMethodName = previewFqName.substringAfterLast(".")
            val previewClass = classLoader.loadClass(previewClassFqName)
            val previewMethod = previewClass.methods.find { it.name == previewMethodName }
                ?: error("Could not find method '$previewMethodName' in class '${previewClass.canonicalName}'")

            val content = previewMethod.invoke(previewClass)
            val previewAnnotation = previewMethod.annotations.find { it.annotationClass.qualifiedName == PREVIEW_ANNOTATION_FQ_NAME }
                ?: error("Could not find '$PREVIEW_ANNOTATION_FQ_NAME' annotation on '$previewClassFqName#$previewMethodName'")
            val environmentKClassProperty = previewAnnotation.annotationClass.members.find { it is KProperty1<*, *> && it.name == "environment" }
                as KProperty1<Any, Class<*>>
            val environmentClass = environmentKClassProperty.get(previewAnnotation)
            val previewEnvironment = environmentClass
                .getDeclaredConstructor()
                .newInstance()
            val showMethod = previewEnvironment.javaClass
                .methods.find { it.name == "show" }
                ?: error("Could not find 'show' in class '${environmentClass.canonicalName}'")
            showMethod.invoke(previewEnvironment, content)
        }
    }
}