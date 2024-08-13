/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.jetbrains.compose.internal.utils.new
import javax.inject.Inject

abstract class JvmApplicationBuildTypes @Inject constructor(
    objects: ObjectFactory
) {
    /**
     * The default build type does not have a classifier
     * to preserve compatibility with tasks, existing before
     * the introduction of the release build type,
     * e.g. we don't want to break existing packageDmg,
     * createDistributable tasks after the introduction
     * of packageReleaseDmg and createReleaseDistributable tasks.
     */
    internal val default: JvmApplicationBuildType = objects.new("")

    val release: JvmApplicationBuildType = objects.new<JvmApplicationBuildType>("release").apply {
        proguard.isEnabled.set(true)
    }
    fun release(fn: Action<JvmApplicationBuildType>) {
        fn.execute(release)
    }
}

abstract class JvmApplicationBuildType @Inject constructor(
    /**
     * A classifier distinguishes tasks and directories of one build type from another.
     * E.g. `release` build type produces packageReleaseDmg task.
     */
    internal val classifier: String,
    objects: ObjectFactory,
) {
    val proguard: ProguardSettings = objects.new()
    fun proguard(fn: Action<ProguardSettings>) {
        fn.execute(proguard)
    }
}
