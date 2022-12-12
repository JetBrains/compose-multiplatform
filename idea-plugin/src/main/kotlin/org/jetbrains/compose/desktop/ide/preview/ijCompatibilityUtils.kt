/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.AbstractNamedData
import com.intellij.openapi.project.Project
import java.lang.reflect.Modifier
import java.util.concurrent.Callable

// todo: filter only Compose projects
internal fun isPreviewCompatible(project: Project): Boolean =
    !LightEdit.owns(project)

internal val kotlinTargetDataKey: Key<out AbstractNamedData> = run {
    val kotlinTargetDataClass = try {
        Class.forName("org.jetbrains.kotlin.idea.gradle.configuration.KotlinTargetData")
    } catch (e: ClassNotFoundException) {
        try {
            Class.forName("org.jetbrains.kotlin.idea.configuration.KotlinTargetData")
        } catch (e: ClassNotFoundException) {
            error("Could not find 'KotlinTargetData' class")
        }
    }
    val companionField = kotlinTargetDataClass.fields.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == "Companion" }
        ?: error("'${kotlinTargetDataClass.canonicalName}.Companion")
    val companionInstance = companionField.get(kotlinTargetDataClass)
    val companionClass = companionInstance.javaClass
    val getKeyMethod = companionClass.methods.firstOrNull { it.name == "getKEY" }
        ?: error("Cannot find '${kotlinTargetDataClass.canonicalName}.Companion.getKEY'")
    @Suppress("UNCHECKED_CAST")
    getKeyMethod.invoke(companionInstance) as Key<out AbstractNamedData>
}

internal inline fun runNonBlocking(crossinline fn: () -> Unit): NonBlockingReadAction<Void?> =
    ReadAction.nonBlocking(Callable<Void?> {
        fn()
        null
    })