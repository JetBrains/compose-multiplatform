/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import java.util.*

enum class ComposePlatforms(vararg val alternativeNames: String) {
    KotlinMultiplatform("Common"),
    Desktop("Jvm"),
    AndroidDebug("Android"),
    AndroidRelease("Android"),
    Js("Web"),
    MacosX64("Macos"),
    MacosArm64("Macos"),
    UikitX64("UiKit"),
    UikitArm64("UiKit"),
    UikitSimArm64("UiKit"),
    TvosArm64("TvOs"),
    TvosX64("TvOs"),
    TvosSimulatorArm64("TvOs"),
    WatchosArm64("WatchOs"),
    WatchosArm32("WatchOs"),
    WatchosX86("WatchOs"),
    WatchosX64("WatchOs"),
    WatchosSimulatorArm64("WatchOs"),
    LinuxX64("Linux"),
    MingwX64("Mingw"),
    ;

    fun matches(nameCandidate: String): Boolean =
        listOf(name, *alternativeNames).any { it.equals(nameCandidate, ignoreCase = true) }

    companion object {
        val ALL = EnumSet.allOf(ComposePlatforms::class.java)

        val JVM_BASED = EnumSet.of(
            ComposePlatforms.Desktop,
            ComposePlatforms.AndroidDebug,
            ComposePlatforms.AndroidRelease
        )

        val ANDROID = EnumSet.of(
            ComposePlatforms.AndroidDebug,
            ComposePlatforms.AndroidRelease
        )

        // These platforms are not supported by skiko yet
        val NO_SKIKO = EnumSet.of(
            ComposePlatforms.TvosArm64,
            ComposePlatforms.TvosX64,
            ComposePlatforms.TvosSimulatorArm64,
            ComposePlatforms.WatchosArm64,
            ComposePlatforms.WatchosArm32,
            ComposePlatforms.WatchosX86,
            ComposePlatforms.WatchosX64,
            ComposePlatforms.WatchosSimulatorArm64,
            ComposePlatforms.LinuxX64,
            ComposePlatforms.MingwX64,
        )

        /**
         * Maps comma separated list of platforms into a set of [ComposePlatforms]
         * The function is case- and whitespace-insensetive.
         *
         * Special value: all
         */
        fun parse(platformsNames: String): Set<ComposePlatforms> {
            val platforms = EnumSet.noneOf(ComposePlatforms::class.java)
            val unknownNames = arrayListOf<String>()

            for (name in platformsNames.split(",").map { it.trim() }) {
                if (name.equals("all", ignoreCase = true)) {
                    return ALL
                }

                val matchingPlatforms = ALL.filter { it.matches(name) }
                if (matchingPlatforms.isNotEmpty()) {
                    platforms.addAll(matchingPlatforms)
                } else {
                    unknownNames.add(name)
                }
            }

            if (unknownNames.isNotEmpty()) {
                error("Unknown platforms: ${unknownNames.joinToString(", ")}")
            }

            return platforms
        }
    }
}
