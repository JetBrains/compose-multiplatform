/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import org.gradle.api.Project
import org.gradle.api.provider.Provider

@Suppress("unused") // public api
class MavenCentralProperties(private val myProject: Project) {
    val coordinates: Provider<String> =
        propertyProvider("maven.central.coordinates")

    val deployName: Provider<String> =
        propertyProvider("maven.central.deployName")

    val user: Provider<String> =
        propertyProvider("maven.central.user", envVar = "MAVEN_CENTRAL_USER")

    val password: Provider<String> =
        propertyProvider("maven.central.password", envVar = "MAVEN_CENTRAL_PASSWORD")

    val publishAfterUploading: Provider<Boolean> =
        propertyProvider("maven.central.publishAfterUploading", defaultValue = "false")
            .map { it.toBoolean() }

    val signArtifacts: Boolean
        get() = myProject.findProperty("maven.central.sign") == "true"

    val signArtifactsKey: Provider<String> =
        propertyProvider("maven.central.sign.key", envVar = "MAVEN_CENTRAL_SIGN_KEY")

    val signArtifactsPassword: Provider<String> =
        propertyProvider("maven.central.sign.password", envVar = "MAVEN_CENTRAL_SIGN_PASSWORD")

    private fun propertyProvider(
        property: String,
        envVar: String? = null,
        defaultValue: String? = null
    ): Provider<String> {
        val providers = myProject.providers
        var result = providers.gradleProperty(property)
        if (envVar != null) {
            result = result.orElse(providers.environmentVariable(envVar))
        }
        result = if (defaultValue != null) {
            result.orElse(defaultValue)
        } else {
            result.orElse(providers.provider {
                val envVarMessage = if (envVar != null) " or '$envVar' environment variable" else ""
                error("Provide value for '$property' Gradle property$envVarMessage")
            })
        }
        return result
    }
}