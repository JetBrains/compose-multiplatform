/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.AbstractNamedData
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

internal val kotlinTargetDataKey: Key<out AbstractNamedData> = run {
    val klass = try {
        Class.forName("org.jetbrains.kotlin.idea.gradle.configuration.KotlinTargetData")
    } catch (e: ClassNotFoundException) {
        try {
            Class.forName("org.jetbrains.kotlin.idea.configuration.KotlinTargetData")
        } catch (e: ClassNotFoundException) {
            error("Could not find 'KotlinTargetData' class")
        }
    }.kotlin
    val companionKlass = klass.companionObject
        ?: error("Cannot find '${klass.qualifiedName}.Companion")
    val keyProperty = companionKlass.memberProperties.find { it.name == "KEY" }
        ?: error("Cannot find '${klass.qualifiedName}.Companion.KEY'")
    @Suppress("UNCHECKED_CAST")
    (keyProperty as KProperty1<Any, Key<out AbstractNamedData>>)
        .get(klass.companionObjectInstance!!)
}