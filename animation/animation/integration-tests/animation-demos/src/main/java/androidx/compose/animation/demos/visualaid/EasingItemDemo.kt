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

package androidx.compose.animation.demos.visualaid

import androidx.compose.animation.core.Easing

enum class EasingItemDemo(val description: String, val function: Easing) {

    Ease("Ease", function = androidx.compose.animation.core.Ease),
    EaseOut("EaseOut", function = androidx.compose.animation.core.EaseOut),
    EaseIn("EaseIn", function = androidx.compose.animation.core.EaseIn),
    EaseInOut("EaseInOut", function = androidx.compose.animation.core.EaseInOut),
    EaseInSine("EaseInSine", function = androidx.compose.animation.core.EaseInSine),
    EaseOutSine(
        "EaseOutSine",
        function = androidx.compose.animation.core.EaseOutSine
    ),
    EaseInOutSine(
        "EaseInOutSine",
        function = androidx.compose.animation.core.EaseInOutSine
    ),
    EaseInCubic(
        "EaseInCubic",
        function = androidx.compose.animation.core.EaseInCubic
    ),
    EaseOutCubic(
        "EaseOutCubic",
        function = androidx.compose.animation.core.EaseOutCubic
    ),
    EaseInOutCubic(
        "EaseInOutCubic",
        function = androidx.compose.animation.core.EaseInOutCubic
    ),

    EaseInQuint(
        "EaseInQuint",
        function = androidx.compose.animation.core.EaseInQuint
    ),
    EaseOutQuint(
        "EaseOutQuint",
        function = androidx.compose.animation.core.EaseOutQuint
    ),
    EaseInOutQuint(
        "EaseInOutQuint",
        function = androidx.compose.animation.core.EaseInOutQuint
    ),

    EaseInCirc("EaseInCirc", function = androidx.compose.animation.core.EaseInCirc),
    EaseOutCirc(
        "EaseOutCirc",
        function = androidx.compose.animation.core.EaseOutCirc
    ),
    EaseInOutCirc(
        "EaseInOutCirc",
        function = androidx.compose.animation.core.EaseInOutCirc
    ),

    EaseInQuad("EaseInQuad", function = androidx.compose.animation.core.EaseInQuad),
    EaseOutQuad(
        "EaseOutQuad",
        function = androidx.compose.animation.core.EaseOutQuad
    ),
    EaseInOutQuad(
        "EaseInOutQuad",
        function = androidx.compose.animation.core.EaseInOutQuad
    ),

    EaseInQuart(
        "EaseInQuart",
        function = androidx.compose.animation.core.EaseInQuart
    ),
    EaseOutQuart(
        "EaseOutQuart",
        function = androidx.compose.animation.core.EaseOutQuart
    ),
    EaseInOutQuart(
        "EaseInOutQuart",
        function = androidx.compose.animation.core.EaseInOutQuart
    ),

    EaseInExpo("EaseInExpo", function = androidx.compose.animation.core.EaseInExpo),
    EaseOutExpo(
        "EaseOutExpo",
        function = androidx.compose.animation.core.EaseOutExpo
    ),
    EaseInOutExpo(
        "EaseInOutExpo",
        function = androidx.compose.animation.core.EaseInOutExpo
    ),

    EaseInBack("EaseInBack", function = androidx.compose.animation.core.EaseInBack),
    EaseOutBack(
        "EaseOutBack",
        function = androidx.compose.animation.core.EaseOutBack
    ),
    EaseInOutBack(
        "EaseInOutBack",
        function = androidx.compose.animation.core.EaseInOutBack
    ),

    EaseInElastic(
        "EaseInElastic",
        function = androidx.compose.animation.core.EaseInElastic
    ),
    EaseOutElastic(
        "EaseOutElastic",
        function = androidx.compose.animation.core.EaseOutElastic
    ),
    EaseInOutElastic(
        "EaseInOutElastic",
        function = androidx.compose.animation.core.EaseInOutElastic
    ),

    EaseOutBounce(
        "EaseOutBounce",
        function = androidx.compose.animation.core.EaseOutBounce
    ),
    EaseInBounce(
        "EaseInBounce",
        function = androidx.compose.animation.core.EaseInBounce
    ),
    EaseInOutBounce(
        "EaseInOutBounce",
        function = androidx.compose.animation.core.EaseInOutBounce
    ),

    Linear("Linear", function = androidx.compose.animation.core.LinearEasing),
}
