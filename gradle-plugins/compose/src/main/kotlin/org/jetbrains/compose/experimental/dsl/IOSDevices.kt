/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

/**
 * iOS device type
 * xcrun simctl list devices
 */
@Suppress("unused")
public enum class IOSDevices(val id: String) {
    IPHONE_6S("com.apple.CoreSimulator.SimDeviceType.iPhone-6s"),
    IPHONE_6S_PLUS("com.apple.CoreSimulator.SimDeviceType.iPhone-6s-Plus"),
    IPHONE_SE("com.apple.CoreSimulator.SimDeviceType.iPhone-SE"),
    IPHONE_7("com.apple.CoreSimulator.SimDeviceType.iPhone-7"),
    IPHONE_7_PLUS("com.apple.CoreSimulator.SimDeviceType.iPhone-7-Plus"),
    IPHONE_8("com.apple.CoreSimulator.SimDeviceType.iPhone-8"),
    IPHONE_8_PLUS("com.apple.CoreSimulator.SimDeviceType.iPhone-8-Plus"),
    IPHONE_X("com.apple.CoreSimulator.SimDeviceType.iPhone-X"),
    IPHONE_XS("com.apple.CoreSimulator.SimDeviceType.iPhone-XS"),
    IPHONE_XS_MAX("com.apple.CoreSimulator.SimDeviceType.iPhone-XS-Max"),
    IPHONE_XR("com.apple.CoreSimulator.SimDeviceType.iPhone-XR"),
    IPHONE_11("com.apple.CoreSimulator.SimDeviceType.iPhone-11"),
    IPHONE_11_PRO("com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro"),
    IPHONE_11_PRO_MAX("com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro-Max"),
    IPHONE_SE_2nd_Gen("com.apple.CoreSimulator.SimDeviceType.iPhone-SE--2nd-generation-"),
    IPHONE_12_MINI("com.apple.CoreSimulator.SimDeviceType.iPhone-12-mini"),
    IPHONE_12("com.apple.CoreSimulator.SimDeviceType.iPhone-12"),
    IPHONE_12_PRO("com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro"),
    IPHONE_12_PRO_MAX("com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"),
    IPHONE_13_PRO("com.apple.CoreSimulator.SimDeviceType.iPhone-13-Pro"),
    IPHONE_13_PRO_MAX("com.apple.CoreSimulator.SimDeviceType.iPhone-13-Pro-Max"),
    IPHONE_13_MINI("com.apple.CoreSimulator.SimDeviceType.iPhone-13-mini"),
    IPHONE_13("com.apple.CoreSimulator.SimDeviceType.iPhone-13"),
    IPOD_TOUCH_7th_Gen("com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-"),
    IPAD_MINI_4("com.apple.CoreSimulator.SimDeviceType.iPad-mini-4"),
    IPAD_AIR_2("com.apple.CoreSimulator.SimDeviceType.iPad-Air-2"),
    IPAD_PRO_9_7_INCH("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--9-7-inch-"),
    IPAD_PRO("com.apple.CoreSimulator.SimDeviceType.iPad-Pro"),
    IPAD_5th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad--5th-generation-"),
    IPAD_PRO_12_9_INCH_2nd_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---2nd-generation-"),
    IPAD_PRO_10_5_INCH("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--10-5-inch-"),
    IPAD_6th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad--6th-generation-"),
    IPAD_7th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad--7th-generation-"),
    IPAD_PRO_11_INCH("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--11-inch-"),
    IPAD_PRO_12_9_INCH_3rd_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---3rd-generation-"),
    IPAD_PRO_11_INCH_2nd_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--11-inch---2nd-generation-"),
    IPAD_PRO_12_9_INCH_4th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---4th-generation-"),
    IPAD_MINI_5th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-mini--5th-generation-"),
    IPAD_AIR_3th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Air--3rd-generation-"),
    IPAD_8th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad--8th-generation-"),
    IPAD_9th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-9th-generation"),
    IPAD_AIR_4th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Air--4th-generation-"),
    IPAD_PRO_11_INCH_3rd_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-3rd-generation"),
    IPAD_12_9_INCH_5th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-5th-generation"),
    IPAD_MINI_6th_Gen("com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"),
}
