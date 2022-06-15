/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.animation.core

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Easing Curve that speeds up quickly and ends slowly.
 *
 * ![Ease Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease.gif)
 */
val Ease: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/**
 * Easing Curve that starts quickly and ends slowly.
 *
 * ![EaseOut Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out.gif)
 */
val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/**
 * Easing Curve that starts slowly and ends quickly.
 *
 * ![EaseIn Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in.gif)
 */
val EaseIn: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)

/**
 * Easing Curve that starts slowly, speeds up and then ends slowly.
 *
 * ![EaseInOut Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out.gif)
 */
val EaseInOut: Easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

/**
 * Easing Curve that starts slowly and ends quickly. Similar to EaseIn, but with slightly less abrupt beginning
 *
 *  ![EaseInSine Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out.gif)
 */
val EaseInSine: Easing = CubicBezierEasing(0.12f, 0f, 0.39f, 0f)

/**
 *  ![EaseOutSine Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_sine.gif)
 */
val EaseOutSine: Easing = CubicBezierEasing(0.61f, 1f, 0.88f, 1f)

/**
 *  ![EaseInOutSine Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_sine.gif)
 */
val EaseInOutSine: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 *  ![EaseInCubic Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_cubic.gif)
 */
val EaseInCubic: Easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

/**
 *  ![EaseOutCubic Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_cubic.gif)
 */
val EaseOutCubic: Easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/**
 *  ![EaseInOutCubic Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_cubic.gif)
 */
val EaseInOutCubic: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/**
 *  ![EaseInQuint Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_quint.gif)
 */
val EaseInQuint: Easing = CubicBezierEasing(0.64f, 0f, 0.78f, 0f)

/**
 *  ![EaseOutQuint Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_quint.gif)
 */
val EaseOutQuint: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 *  ![EaseInOutQuint Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_quint.gif)
 */
val EaseInOutQuint: Easing = CubicBezierEasing(0.83f, 0f, 0.17f, 1f)

/**
 *  ![EaseInCirc Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_circ.gif)
 */
val EaseInCirc: Easing = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)

/**
 *  ![EaseOutCirc Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_circ.gif)
 */
val EaseOutCirc: Easing = CubicBezierEasing(0f, 0.55f, 0.45f, 1f)

/**
 *  ![EaseInOutCirc Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_circ.gif)
 */
val EaseInOutCirc: Easing = CubicBezierEasing(0.85f, 0f, 0.15f, 1f)

/**
 *  ![EaseInQuad Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_quad.gif)
 */
val EaseInQuad: Easing = CubicBezierEasing(0.11f, 0f, 0.5f, 0f)

/**
 *  ![EaseOutQuad Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_quad.gif)
 */
val EaseOutQuad: Easing = CubicBezierEasing(0.5f, 1f, 0.89f, 1f)

/**
 *  ![EaseInOutQuad Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_quad.gif)
 */
val EaseInOutQuad: Easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

/**
 *  ![EaseInQuart Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_quart.gif)
 */
val EaseInQuart: Easing = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)

/**
 *  ![EaseOutQuart Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_quart.gif)
 */
val EaseOutQuart: Easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

/**
 *  ![EaseInOutQuart Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_quart.gif)
 */
val EaseInOutQuart: Easing = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)

/**
 *  ![EaseInExpo Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_expo.gif)
 */
val EaseInExpo: Easing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

/**
 *  ![EaseOutExpo Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_expo.gif)
 */
val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 *  ![EaseInOutExpo Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_expo.gif)
 */
val EaseInOutExpo: Easing = CubicBezierEasing(0.87f, 0f, 0.13f, 1f)

/**
 *  ![EaseInBack Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_back.gif)
 */
val EaseInBack: Easing = CubicBezierEasing(0.36f, 0f, 0.66f, -0.56f)

/**
 *  ![EaseOutBack Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_back.gif)
 */
val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 *  ![EaseInOutBack Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_back.gif)
 */
val EaseInOutBack: Easing = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)

/**
 *  ![EaseInElastic Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_elastic.gif)
 */
val EaseInElastic: Easing = Easing { fraction: Float ->
    val c4 = (2f * PI) / 3f

    return@Easing when (fraction) {
        0f -> 0f
        1f -> 1f
        else ->
            (-(2.0f).pow(10f * fraction - 10.0f) *
                sin((fraction * 10f - 10.75f) * c4)).toFloat()
    }
}

/**
 *  ![EaseOutElastic Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_elastic.gif)
 */
val EaseOutElastic: Easing = Easing { fraction ->
    val c4 = (2f * PI) / 3f

    return@Easing when (fraction) {
        0f -> 0f
        1f -> 1f
        else ->
            ((2.0f).pow(-10.0f * fraction) *
                sin((fraction * 10f - 0.75f) * c4) + 1f).toFloat()
    }
}

/**
 *  ![EaseInOutElastic Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_elastic.gif)
 */
val EaseInOutElastic: Easing = Easing { fraction ->
    val c5 = (2f * PI) / 4.5f
    return@Easing when (fraction) {
        0f -> 0f
        1f -> 1f
        in 0f..0.5f ->
            (-(2.0f.pow(20.0f * fraction - 10.0f) *
                sin((20.0f * fraction - 11.125f) * c5)) / 2.0f).toFloat()
        else ->
            ((2.0f.pow(-20.0f * fraction + 10.0f) *
                sin((fraction * 20f - 11.125f) * c5)) / 2f).toFloat() + 1f
    }
}

/**
 *  ![EaseOutBounce Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_out_bounce.gif)
 */
val EaseOutBounce: Easing = Easing { fraction ->
    val n1 = 7.5625f
    val d1 = 2.75f
    var newFraction = fraction

    return@Easing if (newFraction < 1f / d1) {
        n1 * newFraction * newFraction
    } else if (newFraction < 2f / d1) {
        newFraction -= 1.5f / d1
        n1 * newFraction * newFraction + 0.75f
    } else if (newFraction < 2.5f / d1) {
        newFraction -= 2.25f / d1
        n1 * newFraction * newFraction + 0.9375f
    } else {
        newFraction -= 2.625f / d1
        n1 * newFraction * newFraction + 0.984375f
    }
}

/**
 *  ![EaseInBounce Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_bounce.gif)
 */
val EaseInBounce: Easing = Easing { fraction ->
    return@Easing 1 - EaseOutBounce.transform(1f - fraction)
}

/**
 *  ![EaseInOutBounce Curve](https://developer.android.com/images/reference/androidx/compose/animation-core/ease_in_out_bounce.gif)
 */
val EaseInOutBounce: Easing = Easing { fraction ->
    return@Easing if (fraction < 0.5) {
        (1 - EaseOutBounce.transform(1f - 2f * fraction)) / 2f
    } else {
        (1 + EaseOutBounce.transform((2f * fraction - 1f))) / 2f
    }
}
