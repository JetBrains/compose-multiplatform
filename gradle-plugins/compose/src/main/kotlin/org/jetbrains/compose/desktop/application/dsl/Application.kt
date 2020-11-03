package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.util.*
import javax.inject.Inject

open class Application @Inject constructor(
    @Suppress("unused")
    val name: String,
    objects: ObjectFactory
) {
    internal var _configurationSource: ConfigurationSource? = null
        private set
    internal var _isDefaultConfigurationEnabled = true
        private set
    internal val _fromFiles = objects.fileCollection()
    internal val _dependenciesTaskNames = ArrayList<String>()

    fun from(from: SourceSet) {
        _configurationSource = ConfigurationSource.GradleSourceSet(from)
    }
    fun from(from: KotlinTarget) {
        check(from is KotlinJvmTarget) { "Non JVM Kotlin MPP targets are not supported: ${from.javaClass.canonicalName} " +
                "is not subtype of ${KotlinJvmTarget::class.java.canonicalName}" }
        _configurationSource = ConfigurationSource.KotlinMppTarget(from)
    }
    fun disableDefaultConfiguration() {
        _isDefaultConfigurationEnabled = false
    }

    fun fromFiles(vararg files: Any) {
        _fromFiles.from(*files)
    }

    fun dependsOn(vararg tasks: String) {
        _dependenciesTaskNames.addAll(tasks)
    }
    fun dependsOn(vararg tasks: Task) {
        tasks.mapTo(_dependenciesTaskNames) { it.path }
    }

    var mainClass: String? = null
    val mainJar: RegularFileProperty = objects.fileProperty()
    var javaHome: String? = null

    val args: MutableList<String> = ArrayList()
    fun args(vararg args: String) {
        this.args.addAll(args)
    }

    val jvmArgs: MutableList<String> = ArrayList()
    fun jvmArgs(vararg jvmArgs: String) {
        this.jvmArgs.addAll(jvmArgs)
    }

    val nativeDistributions: NativeDistributions = objects.newInstance(NativeDistributions::class.java)
    fun nativeDistributions(fn: Action<NativeDistributions>) {
        fn.execute(nativeDistributions)
    }
}
