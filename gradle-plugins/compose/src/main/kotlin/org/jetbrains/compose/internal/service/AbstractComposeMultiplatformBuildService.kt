/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.service

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistration
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

// The service implements OperationCompletionListener just so Gradle would materialize the service even if the service is not used by any task or transformation
 abstract class AbstractComposeMultiplatformBuildService<P : BuildServiceParameters> : BuildService<P> , OperationCompletionListener, AutoCloseable {
    override fun onFinish(event: FinishEvent) {}
    override fun close() {}
}

internal inline fun <reified Service : BuildService<*>> serviceName(instance: Service? = null): String =
    fqName(instance)

internal inline fun <reified Service : AbstractComposeMultiplatformBuildService<Params>, reified Params : BuildServiceParameters> registerServiceIfAbsent(
    project: Project,
    crossinline initParams: Params.() -> Unit = {}
): BuildServiceRegistration<Service, Params> {
    if (findRegistration<Service, Params>(project) == null) {
        val newService = project.gradle.sharedServices.registerIfAbsent(fqName<Service>(), Service::class.java) {
            it.parameters.initParams()
        }
        // Workaround to materialize a service even if it is not bound to a task
        BuildEventsListenerRegistryProvider.getInstance(project).onTaskCompletion(newService)
    }

    return getExistingServiceRegistration(project)
}

internal inline fun <reified Service : BuildService<Params>, reified Params : BuildServiceParameters> getExistingServiceRegistration(
    project: Project
): BuildServiceRegistration<Service, Params> {
    val registration = findRegistration<Service, Params>(project)
        ?: error("Service '${serviceName<Service>()}' was not initialized")
    return registration.verified(project)
}

private inline fun <reified Service : BuildService<Params>, reified Params : BuildServiceParameters> BuildServiceRegistration<*, *>.verified(
    project: Project
): BuildServiceRegistration<Service, Params> {
    val parameters = parameters
    // We are checking the type of parameters instead of the type of service
    // to avoid materializing the service.
    // After a service instance is created all changes made to its parameters won't be visible to
    // that particular service instance.
    // This is undesirable in some cases. For example, when reporting configuration problems,
    // we want to collect all configuration issues from all projects first, then report issues all at once
    // in execution phase.
    if (parameters !is Params) {
        // Compose Gradle plugin was probably loaded more than once
        // See https://github.com/JetBrains/compose-multiplatform/issues/3459
        if (fqName(parameters) == fqName<Params>()) {
            val rootScript = project.rootProject.buildFile
            error("""
                Compose Multiplatform Gradle plugin has been loaded in multiple classloaders.
                To avoid classloading issues, declare Compose Gradle Plugin in root build file $rootScript.
            """.trimIndent())
        } else {
            error("Shared build service '${serviceName<Service>()}' parameters have unexpected type: ${fqName(parameters)}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    return this as BuildServiceRegistration<Service, Params>
}

private inline fun <reified S : BuildService<P>, reified P : BuildServiceParameters> findRegistration(
    project: Project
): BuildServiceRegistration<*, *>? =
    project.gradle.sharedServices.registrations.findByName(fqName<S>())

private inline fun <reified T : Any> fqName(instance: T? = null) = T::class.java.canonicalName
