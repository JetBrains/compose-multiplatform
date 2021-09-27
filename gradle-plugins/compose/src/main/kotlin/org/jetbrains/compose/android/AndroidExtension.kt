package org.jetbrains.compose.android


import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class AndroidExtension @Inject constructor(private val objectFactory: ObjectFactory) : ExtensionAware {
    @Input
    var useAndroidX : Boolean = false
    @Input
    var androidxVersion: String = "1.0.1" //should be set by CI, but could be overridden in build script
    @Input
    var mppSolutionGroup: String = "org.jetbrains.compose" //it is added for future use by Google
}