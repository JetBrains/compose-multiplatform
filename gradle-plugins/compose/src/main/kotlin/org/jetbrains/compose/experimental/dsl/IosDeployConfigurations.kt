/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

open class IosDeployConfigurations @Inject constructor(
    val objects: ObjectFactory
) {
    internal val deployTargets: MutableList<DeployTargetWithId> = mutableListOf()
    public fun simulator(id: String, configureSimulator: Action<DeployTarget.Simulator>) {
        val currentSimulator = objects.newInstance(DeployTarget.Simulator::class.java)
        configureSimulator.execute(currentSimulator)
        deployTargets.add(DeployTargetWithId(id, currentSimulator))
    }

    public fun localFile(id: String, configureLocalFile: Action<DeployTarget.LocalFile>) {
        val current = objects.newInstance(DeployTarget.LocalFile::class.java)
        configureLocalFile.execute(current)
        deployTargets.add(DeployTargetWithId(id, current))
    }

    public fun connectedDevice(id: String, configureConnectedDevice: Action<DeployTarget.ConnectedDevice>) {
        val current = objects.newInstance(DeployTarget.ConnectedDevice::class.java)
        configureConnectedDevice.execute(current)
        deployTargets.add(DeployTargetWithId(id, current))
    }

}

sealed interface DeployTarget {
    open class Simulator : DeployTarget {
        var device: IOSDevices = IOSDevices.IPHONE_8
    }

    open class LocalFile : DeployTarget {
        var outputFile: File? = null
    }

    open class ConnectedDevice : DeployTarget {
        var teamId: String? = null
    }
}

internal class DeployTargetWithId(
    val id: String,
    val deploy: DeployTarget
)
