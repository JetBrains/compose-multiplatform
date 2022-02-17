/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.compose.experimental.uikit.internal

import kotlinx.serialization.Serializable

@Serializable
internal class SimctlListData(
    val devicetypes: List<DeviceTypeData>,
    val runtimes: List<RuntimeData>,
    val devices: Map<String, List<DeviceData>>,
    val pairs: Map<String, WatchAndPhonePairData>,
)

@Serializable
internal class DeviceTypeData(
    val name: String,
    val minRuntimeVersion: Long,
    val bundlePath: String,
    val maxRuntimeVersion: Long,
    val identifier: String,
    val productFamily: String
)

@Serializable
internal class RuntimeData(
    val name: String,
    val bundlePath: String,
    val buildversion: String,
    val runtimeRoot: String,
    val identifier: String,
    val version: String,
    val isAvailable: Boolean,
    val supportedDeviceTypes: List<SupportedDeviceTypeData>
)

@Serializable
internal class SupportedDeviceTypeData(
    val bundlePath: String,
    val name: String,
    val identifier: String,
    val productFamily: String
)

@Serializable
internal class DeviceData(
    val name: String,
    val availabilityError: String? = null,
    val dataPath: String,
    val dataPathSize: Long,
    val logPath: String,
    val udid: String,
    val isAvailable: Boolean,
    val deviceTypeIdentifier: String,
    val state: String,
)

internal val DeviceData.booted: Boolean
    get() = state == "Booted"

@Serializable
internal class WatchAndPhonePairData(
    val watch: DeviceInPairData,
    val phone: DeviceInPairData
)

@Serializable
internal class DeviceInPairData(
    val name: String,
    val udid: String,
    val state: String,
)
