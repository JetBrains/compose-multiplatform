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

package androidx.compose.foundation

import android.graphics.Point
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PlatformMagnifierTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun androidPlatformMagnifier_showsMagnifier() {
        val magnifier = createAndroidPlatformMagnifier()
        rule.runOnIdle {
            magnifier.update(
                sourceCenter = Offset.Zero,
                magnifierCenter = Offset.Unspecified,
                zoom = Float.NaN
            )
        }

        rule.runOnIdle {
            assertThat(magnifier.magnifier.position).isEqualTo(Point(0, 0))
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun androidPlatformMagnifier_updatesZoom_whenValid() {
        val magnifier = createAndroidPlatformMagnifier()
        rule.runOnIdle {
            magnifier.update(
                sourceCenter = Offset.Zero,
                magnifierCenter = Offset.Unspecified,
                zoom = 1f
            )

            assertThat(magnifier.magnifier.zoom).isEqualTo(1f)

            magnifier.update(
                sourceCenter = Offset.Zero,
                magnifierCenter = Offset.Unspecified,
                zoom = 2f
            )

            assertThat(magnifier.magnifier.zoom).isEqualTo(2f)
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun androidPlatformMagnifier_doesNotUpdateZoom_whenNaN() {
        val magnifier = createAndroidPlatformMagnifier()
        rule.runOnIdle {
            magnifier.update(
                sourceCenter = Offset.Zero,
                magnifierCenter = Offset.Unspecified,
                zoom = 1f
            )

            assertThat(magnifier.magnifier.zoom).isEqualTo(1f)

            magnifier.update(
                sourceCenter = Offset.Zero,
                magnifierCenter = Offset.Unspecified,
                zoom = Float.NaN
            )

            assertThat(magnifier.magnifier.zoom).isEqualTo(1f)
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun androidPlatformMagnifier_specifiesMagnifierCenter_whenSpecified() {
        val magnifier = createAndroidPlatformMagnifier()
        rule.runOnIdle {
            magnifier.update(
                sourceCenter = Offset.Zero,
                magnifierCenter = VIEW_SIZE.center.toOffset(),
                zoom = Float.NaN
            )
        }

        rule.runOnIdle {
            assertThat(magnifier.magnifier.sourcePosition).isEqualTo(Point(0, 0))
            // position is the top-left of the magnifier so we need to offset it.
            assertThat(magnifier.magnifier.position!!.x + magnifier.magnifier.width / 2)
                .isEqualTo(VIEW_SIZE.center.x)
            assertThat(magnifier.magnifier.position!!.y + magnifier.magnifier.height / 2)
                .isEqualTo(VIEW_SIZE.center.y)
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun androidPlatformMagnifier_doesNotSpecifyMagnifierCenter_whenNotSpecified() {
        // To avoid making this test depend on the actual default offset the framework happens to
        // use for the magnifier, we just record the first magnifier position after placing the
        // source, then move the source and check the new position and assert that it moved the
        // same amount.
        val magnifierDelta = IntOffset(10, 10)
        val magnifier = createAndroidPlatformMagnifier()
        rule.runOnIdle {
            magnifier.update(
                sourceCenter = VIEW_SIZE.center.toOffset(),
                magnifierCenter = Offset.Unspecified,
                zoom = Float.NaN
            )
            val initialMagnifierPosition = magnifier.magnifier.position!!.toIntOffset()

            magnifier.update(
                sourceCenter = (VIEW_SIZE.center + magnifierDelta).toOffset(),
                magnifierCenter = Offset.Unspecified,
                zoom = Float.NaN
            )

            assertThat(magnifier.magnifier.position!!.toIntOffset())
                .isEqualTo(initialMagnifierPosition + magnifierDelta)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun androidPlatformMagnifier_returnsDefaultSize() {
        val magnifier = createAndroidPlatformMagnifier()
        assertThat(magnifier.size.width).isGreaterThan(0)
        assertThat(magnifier.size.height).isGreaterThan(0)
    }

    // Size is only configurable on 29+
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun androidPlatformMagnifier_usesRequestedSize() {
        val magnifierSize = IntSize(10, 11)
        val magnifier = with(rule.density) {
            createAndroidPlatformMagnifier(size = magnifierSize.toSize().toDpSize())
        }
        assertThat(magnifier.size).isEqualTo(magnifierSize)
    }

    private fun createAndroidPlatformMagnifier(
        size: DpSize = DpSize.Unspecified
    ): PlatformMagnifierFactoryApi28Impl.PlatformMagnifierImpl {
        lateinit var magnifier: PlatformMagnifierFactoryApi28Impl.PlatformMagnifierImpl
        rule.setContent {
            val dpSize = with(LocalDensity.current) { VIEW_SIZE.toSize().toDpSize() }
            // Force the view to measure to non-zero size to give the magnifier room to show.
            Box(Modifier.requiredSize(dpSize)) {
                val currentView = LocalView.current
                val density = LocalDensity.current

                DisposableEffect(Unit) {
                    magnifier = PlatformMagnifierFactory.getForCurrentPlatform().create(
                        view = currentView,
                        density = density,
                        initialZoom = Float.NaN,
                        style = MagnifierStyle(size = size),
                    ) as PlatformMagnifierFactoryApi28Impl.PlatformMagnifierImpl
                    onDispose {}
                }
            }
        }
        return rule.runOnIdle { magnifier }
    }

    private companion object {
        val VIEW_SIZE = IntSize(500, 500)

        fun Point.toIntOffset() = IntOffset(x, y)
    }
}
