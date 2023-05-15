package org.jetbrains.compose.android


import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class AndroidExtension @Inject constructor(private val objectFactory: ObjectFactory) : ExtensionAware {
}