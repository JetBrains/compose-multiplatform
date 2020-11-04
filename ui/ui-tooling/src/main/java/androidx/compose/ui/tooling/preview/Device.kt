/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.tooling.preview

import androidx.annotation.StringDef

/**
 * List with the pre-defined devices available to be used in the preview.
 */
object Devices {
    const val DEFAULT = ""

    const val NEXUS_7 = "id:Nexus 7"
    const val NEXUS_7_2013 = "id:Nexus 7 2013"
    const val NEXUS_5 = "id:Nexus 5"
    const val NEXUS_6 = "id:Nexus 6"
    const val NEXUS_9 = "id:Nexus 9"
    const val NEXUS_10 = "name:Nexus 10"
    const val NEXUS_5X = "id:Nexus 5X"
    const val NEXUS_6P = "id:Nexus 6P"
    const val PIXEL_C = "id:pixel_c"
    const val PIXEL = "id:pixel"
    const val PIXEL_XL = "id:pixel_xl"
    const val PIXEL_2 = "id:pixel_2"
    const val PIXEL_2_XL = "id:pixel_2_xl"
    const val PIXEL_3 = "id:pixel_3"
    const val PIXEL_3_XL = "id:pixel_3_xl"
    const val PIXEL_3A = "id:pixel_3a"
    const val PIXEL_3A_XL = "id:pixel_3a_xl"
    const val PIXEL_4 = "id:pixel_4"
    const val PIXEL_4_XL = "id:pixel_4_xl"

    const val AUTOMOTIVE_1024p = "id:automotive_1024p_landscape"
}

/**
 * Annotation for defining the [Preview] device to use.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    open = true,
    value = [
        Devices.DEFAULT,

        Devices.NEXUS_7,
        Devices.NEXUS_7_2013,
        Devices.NEXUS_5,
        Devices.NEXUS_6,
        Devices.NEXUS_9,
        Devices.NEXUS_10,
        Devices.NEXUS_5X,
        Devices.NEXUS_6P,
        Devices.PIXEL_C,
        Devices.PIXEL,
        Devices.PIXEL_XL,
        Devices.PIXEL_2,
        Devices.PIXEL_2_XL,
        Devices.PIXEL_3,
        Devices.PIXEL_3_XL,
        Devices.PIXEL_3A,
        Devices.PIXEL_3A_XL,
        Devices.PIXEL_4,
        Devices.PIXEL_4_XL,

        Devices.AUTOMOTIVE_1024p
    ]
)
annotation class Device
