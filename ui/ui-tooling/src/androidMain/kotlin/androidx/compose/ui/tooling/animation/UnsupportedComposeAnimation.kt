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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import org.jetbrains.annotations.TestOnly

/**
 * [ComposeAnimation] of type [ComposeAnimationType.UNSUPPORTED].
 */
internal class UnsupportedComposeAnimation private constructor(
    override val label: String?
) : ComposeAnimation {
    override val type = ComposeAnimationType.UNSUPPORTED
    override val animationObject: Any = 0
    override val states = emptySet<Int>()

    companion object {
        /**
         * [ComposeAnimationType] from ANIMATABLE to UNSUPPORTED are not available in previous
         * versions of the library. To avoid creating non-existing enum,
         * [UnsupportedComposeAnimation] should only be instantiated if [ComposeAnimationType] API
         * for UNSUPPORTED enum is available.
         */
        var apiAvailable = enumValues<ComposeAnimationType>().any { it.name == "UNSUPPORTED" }
            private set

        fun create(label: String?) =
            if (apiAvailable) UnsupportedComposeAnimation(label) else null

        /** This method is for testing only. */
        @TestOnly
        fun testOverrideAvailability(override: Boolean) {
            apiAvailable = override
        }
    }
}