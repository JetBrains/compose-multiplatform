/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

@Suppress("EqualsOrHashCode")
data class CSSAnimation(
    val keyframesName: String,
    var duration: List<CSSSizeValue<out CSSUnitTime>>? = null,
    var timingFunction: List<AnimationTimingFunction>? = null,
    var delay: List<CSSSizeValue<out CSSUnitTime>>? = null,
    var iterationCount: List<Int?>? = null,
    var direction: List<AnimationDirection>? = null,
    var fillMode: List<AnimationFillMode>? = null,
    var playState: List<AnimationPlayState>? = null
) : CSSStyleValue {
    override fun toString(): String {
        val values = listOfNotNull(
            keyframesName,
            duration?.joinToString(", "),
            timingFunction?.joinToString(", "),
            delay?.joinToString(", "),
            iterationCount?.joinToString(", ") { it?.toString() ?: "infinite" },
            direction?.joinToString(", "),
            fillMode?.joinToString(", "),
            playState?.joinToString(", ")
        )
        return values.joinToString(" ")
    }
}

fun CSSAnimation.duration(vararg values: CSSSizeValue<out CSSUnitTime>) {
    this.duration = values.toList()
}

fun CSSAnimation.timingFunction(vararg values: AnimationTimingFunction) {
    this.timingFunction = values.toList()
}

fun CSSAnimation.delay(vararg values: CSSSizeValue<out CSSUnitTime>) {
    this.delay = values.toList()
}

fun CSSAnimation.iterationCount(vararg values: Int?) {
    this.iterationCount = values.toList()
}

fun CSSAnimation.direction(vararg values: AnimationDirection) {
    this.direction = values.toList()
}

fun CSSAnimation.fillMode(vararg values: AnimationFillMode) {
    this.fillMode = values.toList()
}

fun CSSAnimation.playState(vararg values: AnimationPlayState) {
    this.playState = values.toList()
}

fun StyleScope.animation(
    keyframesName: String,
    builder: CSSAnimation.() -> Unit
) {
    val animation = CSSAnimation(keyframesName).apply(builder)
    property("animation", animation)
}

fun StyleScope.animation(
    keyframes: CSSNamedKeyframes,
    builder: CSSAnimation.() -> Unit
) = animation(keyframes.name, builder)


