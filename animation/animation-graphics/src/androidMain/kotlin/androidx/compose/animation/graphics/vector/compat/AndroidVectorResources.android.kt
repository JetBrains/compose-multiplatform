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

package androidx.compose.animation.graphics.vector.compat

/**
 * Constants used to resolve AnimatedVectorDrawable attributes during xml inflation
 */
internal object AndroidVectorResources {

    val STYLEABLE_ANIMATED_VECTOR_DRAWABLE = intArrayOf(android.R.attr.drawable)
    const val STYLEABLE_ANIMATED_VECTOR_DRAWABLE_DRAWABLE = 0

    val STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET =
        intArrayOf(android.R.attr.name, android.R.attr.animation)
    const val STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET_ANIMATION = 1
    const val STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET_NAME = 0

    val STYLEABLE_ANIMATOR = intArrayOf(
        0x01010141,
        0x01010198,
        0x010101be,
        0x010101bf,
        0x010101c0,
        0x010102de,
        0x010102df,
        0x010102e0
    )
    const val STYLEABLE_ANIMATOR_INTERPOLATOR = 0
    const val STYLEABLE_ANIMATOR_DURATION = 1
    const val STYLEABLE_ANIMATOR_START_OFFSET = 2
    const val STYLEABLE_ANIMATOR_REPEAT_COUNT = 3
    const val STYLEABLE_ANIMATOR_REPEAT_MODE = 4
    const val STYLEABLE_ANIMATOR_VALUE_FROM = 5
    const val STYLEABLE_ANIMATOR_VALUE_TO = 6
    const val STYLEABLE_ANIMATOR_VALUE_TYPE = 7

    val STYLEABLE_ANIMATOR_SET = intArrayOf(0x010102e2)
    const val STYLEABLE_ANIMATOR_SET_ORDERING = 0

    val STYLEABLE_PROPERTY_VALUES_HOLDER =
        intArrayOf(0x010102de, 0x010102df, 0x010102e0, 0x010102e1)
    const val STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_FROM = 0
    const val STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TO = 1
    const val STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TYPE = 2
    const val STYLEABLE_PROPERTY_VALUES_HOLDER_PROPERTY_NAME = 3

    val STYLEABLE_KEYFRAME = intArrayOf(0x01010024, 0x01010141, 0x010102e0, 0x010104d8)
    const val STYLEABLE_KEYFRAME_VALUE = 0
    const val STYLEABLE_KEYFRAME_INTERPOLATOR = 1
    const val STYLEABLE_KEYFRAME_VALUE_TYPE = 2
    const val STYLEABLE_KEYFRAME_FRACTION = 3

    val STYLEABLE_PROPERTY_ANIMATOR = intArrayOf(0x010102e1, 0x01010405, 0x01010474, 0x01010475)
    const val STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_NAME = 0
    const val STYLEABLE_PROPERTY_ANIMATOR_PATH_DATA = 1
    const val STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_X_NAME = 2
    const val STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_Y_NAME = 3

    val STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR = intArrayOf(
        android.R.attr.tension,
        android.R.attr.extraTension
    )
    const val STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_TENSION = 0
    const val STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_EXTRA_TENSION = 1

    val STYLEABLE_ACCELERATE_INTERPOLATOR = intArrayOf(android.R.attr.factor)
    const val STYLEABLE_ACCELERATE_INTERPOLATOR_FACTOR = 0

    val STYLEABLE_DECELERATE_INTERPOLATOR = intArrayOf(android.R.attr.factor)
    const val STYLEABLE_DECELERATE_INTERPOLATOR_FACTOR = 0

    val STYLEABLE_CYCLE_INTERPOLATOR = intArrayOf(android.R.attr.cycles)
    const val STYLEABLE_CYCLE_INTERPOLATOR_CYCLES = 0

    val STYLEABLE_OVERSHOOT_INTERPOLATOR = intArrayOf(android.R.attr.tension)
    const val STYLEABLE_OVERSHOOT_INTERPOLATOR_TENSION = 0

    val STYLEABLE_PATH_INTERPOLATOR =
        intArrayOf(0x010103fc, 0x010103fd, 0x010103fe, 0x010103ff, 0x01010405)
    const val STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_1 = 0
    const val STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_1 = 1
    const val STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_2 = 2
    const val STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_2 = 3
    const val STYLEABLE_PATH_INTERPOLATOR_PATH_DATA = 4

    const val FAST_OUT_LINEAR_IN = 0x010c000f
    const val FAST_OUT_SLOW_IN = 0x010c000d
    const val LINEAR_OUT_SLOW_IN = 0x010c000e
}
