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

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_APPLIANCE
import android.content.res.Configuration.UI_MODE_TYPE_CAR
import android.content.res.Configuration.UI_MODE_TYPE_DESK
import android.content.res.Configuration.UI_MODE_TYPE_MASK
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
import android.content.res.Configuration.UI_MODE_TYPE_UNDEFINED
import android.content.res.Configuration.UI_MODE_TYPE_VR_HEADSET
import android.content.res.Configuration.UI_MODE_TYPE_WATCH
import androidx.annotation.IntDef

/**
 * Annotation of setting uiMode in [Preview].
 * @hide
 */
@SuppressLint("UniqueConstants") // UI_MODE_NIGHT_UNDEFINED == UI_MODE_TYPE_UNDEFINED
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value = [
        UI_MODE_TYPE_MASK,
        UI_MODE_TYPE_UNDEFINED,
        UI_MODE_TYPE_APPLIANCE,
        UI_MODE_TYPE_CAR,
        UI_MODE_TYPE_DESK,
        UI_MODE_TYPE_NORMAL,
        UI_MODE_TYPE_TELEVISION,
        UI_MODE_TYPE_VR_HEADSET,
        UI_MODE_TYPE_WATCH,
        UI_MODE_NIGHT_MASK,
        UI_MODE_NIGHT_UNDEFINED,
        UI_MODE_NIGHT_NO,
        UI_MODE_NIGHT_YES
    ]
)
annotation class UiMode
