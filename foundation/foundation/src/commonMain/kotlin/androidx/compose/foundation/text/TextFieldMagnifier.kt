/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.collectLatest

/**
 * Internal property used to report the offset at which a magnifier is being shown, or
 * [Offset.Unspecified] if the magnifier is hidden, for UI tests. This property is only reported
 * if the `isTextMagnifierSemanticsEnabled` parameter to [textFieldMagnifier] is true.
 */
internal val TextFieldMagnifierOffsetProperty =
    SemanticsPropertyKey<Offset>("TextFieldMagnifier")

private val UnspecifiedAnimationVector2D = AnimationVector2D(Float.NaN, Float.NaN)

/** Like `Offset.VectorConverter` but propagates [Offset.Unspecified] values. */
private val UnspecifiedSafeOffsetVectorConverter = TwoWayConverter<Offset, AnimationVector2D>(
    convertToVector = {
        if (it.isSpecified) {
            AnimationVector2D(it.x, it.y)
        } else {
            UnspecifiedAnimationVector2D
        }
    },
    convertFromVector = { Offset(it.v1, it.v2) }
)

private val OffsetDisplacementThreshold = Offset(
    Spring.DefaultDisplacementThreshold,
    Spring.DefaultDisplacementThreshold
)

/**
 * Optionally shows a magnifier widget, if the current platform supports it, for the current state
 * of a [TextFieldSelectionManager]. Should check [TextFieldState.draggingHandle] to see which
 * handle is being dragged and then calculate the magnifier position for that handle.
 *
 * Actual implementations should as much as possible actually live in this common source set, _not_
 * the platform-specific source sets. The actual implementations of this function should then just
 * delegate to those functions (e.g. [textFieldMagnifierAndroidImpl]).
 */
internal expect fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier

/**
 * The Android-specific implementation of a [textFieldMagnifier]. Animates the magnifier position
 * between discrete cursor positions.
 *
 * See `android.widget.Editor` methods for the framework logic that this function copies.
 *
 * @param androidMagnifier A function that returns a modifier that actually shows the Android
 * magnifier widget at the given center offset.
 * @param isTextMagnifierSemanticsEnabled Enables reporting the offset of the text field's magnifier
 * for UI tests. Must be set before the test content is set.
 */
internal fun Modifier.textFieldMagnifierAndroidImpl(
    manager: TextFieldSelectionManager,
    androidMagnifier: (center: () -> Offset) -> Modifier,
    isTextMagnifierSemanticsEnabled: Boolean = false
): Modifier {
    // manager.state is not a snapshot state value, it's just a regular lazily-initialized property
    // that doesn't change after being initialized, so it doesn't need to be read inside a
    // restartable function.
    val state = manager.state ?: return Modifier

    return textFieldMagnifierAndroidImpl(
        draggingHandle = { state.draggingHandle },
        fieldValue = { manager.value },
        transformTextOffset = { manager.offsetMapping.originalToTransformed(it) },
        getCursorRect = { state.layoutResult?.value?.getCursorRect(it) },
        androidMagnifier = androidMagnifier,
        isTextMagnifierSemanticsEnabled = isTextMagnifierSemanticsEnabled
    )
}

/**
 * Separate for testability.
 */
@Suppress("ModifierInspectorInfo")
internal fun Modifier.textFieldMagnifierAndroidImpl(
    draggingHandle: () -> Handle?,
    fieldValue: () -> TextFieldValue,
    transformTextOffset: (Int) -> Int,
    getCursorRect: (offset: Int) -> Rect?,
    androidMagnifier: (center: () -> Offset) -> Modifier,
    isTextMagnifierSemanticsEnabled: Boolean
): Modifier = composed {
    // The magnifier jumps between discreet cursor positions, so animate it to make it easier to
    // visually follow while dragging.
    val animatedMagnifierOffset by rememberAnimatedDerivedStateOf(
        // Can't use Offset.VectorConverter because we need to handle Unspecified specially.
        typeConverter = UnspecifiedSafeOffsetVectorConverter,
        visibilityThreshold = OffsetDisplacementThreshold
    ) offset@{
        val rawTextOffset = when (draggingHandle()) {
            null -> return@offset Offset.Unspecified
            Handle.Cursor,
            Handle.SelectionStart -> fieldValue().selection.start
            Handle.SelectionEnd -> fieldValue().selection.end
        }
        val textOffset = transformTextOffset(rawTextOffset)

        // Center vertically on the current line.
        // If the text hasn't been laid out yet, don't show the modifier.
        return@offset getCursorRect(textOffset)?.center ?: Offset.Unspecified
    }

    return@composed this
        .then(androidMagnifier { /* centerOffset = */ animatedMagnifierOffset })
        .then(
            // Note that this is not a mutable state – tests must enable this property before
            // specifying composable content.
            if (isTextMagnifierSemanticsEnabled) {
                Modifier.semantics {
                    set(TextFieldMagnifierOffsetProperty, animatedMagnifierOffset)
                }
            } else {
                Modifier
            }
        )
}

/**
 * Remembers and returns a [State] that will smoothly animate to the result of [targetCalculation]
 * any time the result of [targetCalculation] changes due to any state values it reads change.
 */
@Composable
private fun <T, V : AnimationVector> rememberAnimatedDerivedStateOf(
    typeConverter: TwoWayConverter<T, V>,
    visibilityThreshold: T? = null,
    animationSpec: AnimationSpec<T> = SpringSpec(visibilityThreshold = visibilityThreshold),
    targetCalculation: () -> T,
): State<T> {
    val targetValue by remember { derivedStateOf(targetCalculation) }
    val animatable = remember {
        Animatable(targetValue, typeConverter, visibilityThreshold)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { targetValue }
            .collectLatest {
                animatable.animateTo(it, animationSpec)
            }
    }
    return animatable.asState()
}
