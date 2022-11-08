/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.compose.experimental.uikit.internal

import kotlinx.serialization.Serializable

@Serializable
internal class SimctlListData(
    val devicetypes: List<DeviceTypeData> = emptyList(),
    val runtimes: List<RuntimeData>,
    val devices: Map<String, List<DeviceData>>,
    val pairs: Map<String, WatchAndPhonePairData> = emptyMap(),
)

@Serializable
internal class DeviceTypeData(
    val name: String? = null,
    val minRuntimeVersion: Long? = null,
    val bundlePath: String? = null,
    val maxRuntimeVersion: Long? = null,
    val identifier: String? = null,
    val productFamily: String? = null
)

@Serializable
internal class RuntimeData(
    val name: String? = null,
    val bundlePath: String? = null,
    val buildversion: String? = null,
    val runtimeRoot: String? = null,
    val identifier: String,
    val version: String,
    val isAvailable: Boolean? = null,
    val supportedDeviceTypes: List<SupportedDeviceTypeData>
)

@Serializable
internal class SupportedDeviceTypeData(
    val bundlePath: String? = null,
    val name: String? = null,
    val identifier: String,
    val productFamily: String? = null
)

@Serializable
internal class DeviceData(
    val name: String,
    val availabilityError: String? = null,
    val dataPath: String? = null,
    val dataPathSize: Long? = null,
    val logPath: String? = null,
    val udid: String,
    /**
     * Simulator may be unavailable after update Xcode version.
     * By default, we think what simulator is available.
     */
    val isAvailable: Boolean = true,
    val deviceTypeIdentifier: String? = null,
    val state: String,
)

internal val DeviceData.booted: Boolean
    get() = state == "Booted"

@Serializable
internal class WatchAndPhonePairData(
    val watch: DeviceInPairData? = null,
    val phone: DeviceInPairData? = null
)

@Serializable
internal class DeviceInPairData(
    val name: String? = null,
    val udid: String? = null,
    val state: String? = null,
)
