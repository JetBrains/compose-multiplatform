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

package androidx.compose.material.ripple

import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [LocalRippleNativeRendering]
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class LocalRippleNativeRenderingTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun usesNativeRipplesByDefault() {
        lateinit var instance: IndicationInstance
        rule.setContent {
            val ripple = rememberRipple()
            val interactionSource = remember { MutableInteractionSource() }
            instance = ripple.rememberUpdatedInstance(interactionSource)
        }

        rule.runOnIdle {
            Truth.assertThat(instance).isInstanceOf(AndroidRippleIndicationInstance::class.java)
        }
    }

    @OptIn(ExperimentalRippleApi::class)
    @Test
    fun usesCommonRipples_whenLocalNativeRippleRenderIsSet() {
        lateinit var instance: IndicationInstance
        rule.setContent {
            CompositionLocalProvider(LocalRippleNativeRendering provides false) {
                val ripple = rememberRipple()
                val interactionSource = remember { MutableInteractionSource() }
                instance = ripple.rememberUpdatedInstance(interactionSource)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(instance).isInstanceOf(CommonRippleIndicationInstance::class.java)
        }
    }
}
