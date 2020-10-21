package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal sealed class ConfigurationSource {
    object None : ConfigurationSource()
    class GradleSourceSet(val sourceSet: SourceSet) : ConfigurationSource()
    class KotlinMppTarget(val target: KotlinJvmTarget) : ConfigurationSource()
}