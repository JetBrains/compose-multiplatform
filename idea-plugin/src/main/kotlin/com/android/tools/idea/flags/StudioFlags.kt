/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.android.tools.idea.flags

object StudioFlags {
    val COMPOSE_EDITOR_SUPPORT = Flag(true)
    val COMPOSE_AUTO_DOCUMENTATION = Flag(true)
    val COMPOSE_FUNCTION_EXTRACTION = Flag(true)
    val COMPOSE_DEPLOY_LIVE_EDIT_USE_EMBEDDED_COMPILER = Flag(true)
    val COMPOSE_COMPLETION_INSERT_HANDLER = Flag(true)
    val COMPOSE_COMPLETION_PRESENTATION = Flag(true)
    val COMPOSE_COMPLETION_WEIGHER = Flag(true)
    val COMPOSE_STATE_OBJECT_CUSTOM_RENDERER = Flag(true)
    val COMPOSE_CONSTRAINTLAYOUT_COMPLETION = Flag(true)
    val SAMPLES_SUPPORT_ENABLED = Flag(true)
}

class Flag<T>(val value: T) {
    fun get(): T = value
}