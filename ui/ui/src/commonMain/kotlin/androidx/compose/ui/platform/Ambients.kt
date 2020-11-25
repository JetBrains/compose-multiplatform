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

package androidx.compose.ui.platform

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.node.Owner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The default animation clock used for animations when an explicit clock isn't provided.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientAnimationClock",
    replaceWith = ReplaceWith(
        "AmbientAnimationClock",
        "androidx.compose.ui.platform.AmbientAnimationClock"
    )
)
val AnimationClockAmbient get() = AmbientAnimationClock

/**
 * The default animation clock used for animations when an explicit clock isn't provided.
 */
val AmbientAnimationClock = staticAmbientOf<AnimationClockObservable>()

/**
 * The ambient that can be used to trigger autofill actions. Eg. [Autofill.requestAutofillForNode].
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientAutofill",
    replaceWith = ReplaceWith(
        "AmbientAutofill",
        "androidx.compose.ui.platform.AmbientAutofill"
    )
)
val AutofillAmbient get() = AmbientAutofill

/**
 * The ambient that can be used to trigger autofill actions. Eg. [Autofill.requestAutofillForNode].
 */
val AmbientAutofill = staticAmbientOf<Autofill?>()

/**
 * The ambient that can be used to add
 * [AutofillNode][import androidx.compose.ui.autofill.AutofillNode]s to the autofill tree. The
 * [AutofillTree] is a temporary data structure that will be replaced by Autofill Semantics
 * (b/138604305).
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientAutofillTree",
    replaceWith = ReplaceWith(
        "AmbientAutofillTree",
        "androidx.compose.ui.platform.AmbientAutofillTree"
    )
)
val AutofillTreeAmbient get() = AmbientAutofillTree

/**
 * The ambient that can be used to add
 * [AutofillNode][import androidx.compose.ui.autofill.AutofillNode]s to the autofill tree. The
 * [AutofillTree] is a temporary data structure that will be replaced by Autofill Semantics
 * (b/138604305).
 */
val AmbientAutofillTree = staticAmbientOf<AutofillTree>()

/**
 * The ambient to provide communication with platform clipboard service.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientClipboardManager",
    replaceWith = ReplaceWith(
        "AmbientClipboardManager",
        "androidx.compose.ui.platform.AmbientClipboardManager"
    )
)
val ClipboardManagerAmbient get() = AmbientClipboardManager

/**
 * The ambient to provide communication with platform clipboard service.
 */
val AmbientClipboardManager = staticAmbientOf<ClipboardManager>()

/**
 * Provides the [Density] to be used to transform between [density-independent pixel
 * units (DP)][androidx.compose.ui.unit.Dp] and [pixel units][androidx.compose.ui.unit.Px] or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and
 * [pixel units][androidx.compose.ui.unit.Px]. This is typically used when a [DP][androidx.compose.ui.unit.Dp]
 * is provided and it must be converted in the body of [Layout] or [DrawModifier].
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientDensity",
    replaceWith = ReplaceWith(
        "AmbientDensity",
        "androidx.compose.ui.platform.AmbientDensity"
    )
)
val DensityAmbient get() = AmbientDensity

/**
 * Provides the [Density] to be used to transform between [density-independent pixel
 * units (DP)][androidx.compose.ui.unit.Dp] and [pixel units][androidx.compose.ui.unit.Px] or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and
 * [pixel units][androidx.compose.ui.unit.Px]. This is typically used when a [DP][androidx.compose.ui.unit.Dp]
 * is provided and it must be converted in the body of [Layout] or [DrawModifier].
 */
val AmbientDensity = staticAmbientOf<Density>()

/**
 * The ambient that can be used to control focus within Compose.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientFocusManager",
    replaceWith = ReplaceWith(
        "AmbientFocusManager",
        "androidx.compose.ui.platform.AmbientFocusManager"
    )
)
@ExperimentalFocus
val FocusManagerAmbient get() = AmbientFocusManager

/**
 * The ambient that can be used to control focus within Compose.
 */
@ExperimentalFocus
val AmbientFocusManager = staticAmbientOf<FocusManager>()

/**
 * The ambient to provide platform font loading methods.
 *
 * Use [androidx.compose.ui.res.fontResource] instead.
 * @suppress
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientFontLoader",
    replaceWith = ReplaceWith(
        "AmbientFontLoader",
        "androidx.compose.ui.platform.AmbientFontLoader"
    )
)
val FontLoaderAmbient get() = AmbientFontLoader

/**
 * The ambient to provide platform font loading methods.
 *
 * Use [androidx.compose.ui.res.fontResource] instead.
 * @suppress
 */
val AmbientFontLoader = staticAmbientOf<Font.ResourceLoader>()

/**
 * The ambient to provide haptic feedback to the user.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientHapticFeedback",
    replaceWith = ReplaceWith(
        "AmbientHapticFeedback",
        "androidx.compose.ui.platform.AmbientHapticFeedback"
    )
)
val HapticFeedBackAmbient get() = AmbientHapticFeedback

/**
 * The ambient to provide haptic feedback to the user.
 */
val AmbientHapticFeedback = staticAmbientOf<HapticFeedback>()

/**
 * The ambient to provide the layout direction.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientLayoutDirection",
    replaceWith = ReplaceWith(
        "AmbientLayoutDirection",
        "androidx.compose.ui.platform.AmbientLayoutDirection"
    )
)
val LayoutDirectionAmbient get() = AmbientLayoutDirection

/**
 * The ambient to provide the layout direction.
 */
val AmbientLayoutDirection = staticAmbientOf<LayoutDirection>()

/**
 * The ambient to provide communication with platform text input service.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientTextInputService",
    replaceWith = ReplaceWith(
        "AmbientTextInputService",
        "androidx.compose.ui.platform.AmbientTextInputService"
    )
)
val TextInputServiceAmbient get() = AmbientTextInputService

/**
 * The ambient to provide communication with platform text input service.
 */
val AmbientTextInputService = staticAmbientOf<TextInputService?>()

/**
 * The ambient to provide text-related toolbar.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientTextToolbar",
    replaceWith = ReplaceWith(
        "AmbientTextToolbar",
        "androidx.compose.ui.platform.AmbientTextToolbar"
    )
)
val TextToolbarAmbient get() = AmbientTextToolbar

/**
 * The ambient to provide text-related toolbar.
 */
val AmbientTextToolbar = staticAmbientOf<TextToolbar>()

/**
 * The ambient to provide functionality related to URL, e.g. open URI.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientUriHandler",
    replaceWith = ReplaceWith(
        "AmbientUriHandler",
        "androidx.compose.ui.platform.AmbientUriHandler"
    )
)
val UriHandlerAmbient get() = AmbientUriHandler

/**
 * The ambient to provide functionality related to URL, e.g. open URI.
 */
val AmbientUriHandler = staticAmbientOf<UriHandler>()

/**
 * The ambient that provides the ViewConfiguration.
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientViewConfiguration",
    replaceWith = ReplaceWith(
        "AmbientViewConfiguration",
        "androidx.compose.ui.platform.AmbientViewConfiguration"
    )
)
val ViewConfigurationAmbient get() = AmbientViewConfiguration

/**
 * The ambient that provides the ViewConfiguration.
 */
val AmbientViewConfiguration = staticAmbientOf<ViewConfiguration>()

/**
 * The ambient that provides information about the window that hosts the current [Owner].
 */
val AmbientWindowManager = staticAmbientOf<WindowManager>()

@OptIn(ExperimentalFocus::class)
@Composable
internal fun ProvideCommonAmbients(
    owner: Owner,
    animationClock: AnimationClockObservable,
    uriHandler: UriHandler,
    content: @Composable () -> Unit
) {
    Providers(
        AmbientAnimationClock provides animationClock,
        AmbientAutofill provides owner.autofill,
        AmbientAutofillTree provides owner.autofillTree,
        AmbientClipboardManager provides owner.clipboardManager,
        AmbientDensity provides owner.density,
        AmbientFocusManager provides owner.focusManager,
        AmbientFontLoader provides owner.fontLoader,
        AmbientHapticFeedback provides owner.hapticFeedBack,
        AmbientLayoutDirection providesDefault owner.layoutDirection,
        AmbientTextInputService provides owner.textInputService,
        AmbientTextToolbar provides owner.textToolbar,
        AmbientUriHandler provides uriHandler,
        AmbientViewConfiguration provides owner.viewConfiguration,
        AmbientWindowManager provides owner.windowManager,
        content = content
    )
}