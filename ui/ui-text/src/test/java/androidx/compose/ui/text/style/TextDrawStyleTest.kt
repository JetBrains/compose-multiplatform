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

package androidx.compose.ui.text.style

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.lerpDiscrete
import androidx.compose.ui.util.lerp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextDrawStyleTest {

    private val defaultBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
    private val TOLERANCE = 1f / 256f

    @Test
    fun `color is not equal to unspecified`() {
        val color = TextDrawStyle.from(Color.Red)
        assertThat(color == TextDrawStyle.Unspecified).isFalse()
    }

    @Test
    fun `brush is not equal to color`() {
        val color = TextDrawStyle.from(Color.Red)
        val brush = TextDrawStyle.from(defaultBrush, 1f)
        assertThat(color == brush).isFalse()
    }

    @Test
    fun `different colors are not equal`() {
        val color = TextDrawStyle.from(Color.Red)
        val otherColor = TextDrawStyle.from(Color.Blue)
        assertThat(color == otherColor).isFalse()
    }

    @Test
    fun `same colors should be equal`() {
        val color = TextDrawStyle.from(Color.Red)
        val otherColor = TextDrawStyle.from(Color.Red)
        assertThat(color == otherColor).isTrue()
    }

    @Test
    fun `unspecified color initiates an Unspecified TextDrawStyle`() {
        val unspecified = TextDrawStyle.from(Color.Unspecified)
        assertThat(unspecified).isEqualTo(TextDrawStyle.Unspecified)
        assertThat(unspecified.color).isEqualTo(Color.Unspecified)
        assertThat(unspecified.brush).isNull()
    }

    @Test
    fun `specified color initiates only color`() {
        val specified = TextDrawStyle.from(Color.Red)
        assertThat(specified.color).isEqualTo(Color.Red)
        assertThat(specified.brush).isNull()
        assertThat(specified.alpha).isEqualTo(1f)
    }

    @Test
    fun `specified color returns its alpha`() {
        val specified = TextDrawStyle.from(Color.Red.copy(alpha = 0.5f))
        assertThat(specified.color).isEqualTo(Color.Red.copy(alpha = 0.5f))
        assertThat(specified.brush).isNull()
        assertThat(specified.alpha).isWithin(TOLERANCE).of(0.5f)
    }

    @Test
    fun `SolidColor is converted to color`() {
        val specified = TextDrawStyle.from(SolidColor(Color.Red), 1f)
        assertThat(specified.color).isEqualTo(Color.Red)
        assertThat(specified.brush).isNull()
    }

    @Test
    fun `SolidColor with alpha is modulated`() {
        val specified = TextDrawStyle.from(SolidColor(Color.Red.copy(alpha = 0.8f)), 0.6f)
        assertThat(specified.color).isEqualTo(Color.Red.copy(alpha = 0.48f))
        assertThat(specified.brush).isNull()
    }

    @Test
    fun `ShaderBrush initiates a brush`() {
        val specified = TextDrawStyle.from(defaultBrush, 0.8f)
        assertThat(specified.color).isEqualTo(Color.Unspecified)
        assertThat(specified.brush).isEqualTo(defaultBrush)
        assertThat(specified.alpha).isWithin(TOLERANCE).of(0.8f)
    }

    @Test
    fun `merging unspecified with anything returns anything`() {
        val current = TextDrawStyle.Unspecified

        val other = TextDrawStyle.from(Color.Red)
        assertThat(current.merge(other).color).isEqualTo(Color.Red)
        assertThat(current.merge(other).brush).isNull()

        val other2 = TextDrawStyle.from(defaultBrush, 1f)
        assertThat(current.merge(other2).color).isEqualTo(Color.Unspecified)
        assertThat(current.merge(other2).brush).isEqualTo(defaultBrush)
    }

    // TODO(b/230787077): Update when Brush is stable.
    @Test
    fun `merging brush with color returns brush`() {
        val current = TextDrawStyle.from(defaultBrush, 1f)

        val other = TextDrawStyle.from(Color.Red)
        assertThat(current.merge(other).color).isEqualTo(Color.Unspecified)
        assertThat(current.merge(other).brush).isEqualTo(defaultBrush)
    }

    @Test
    fun `merging color with brush returns brush`() {
        val current = TextDrawStyle.from(Color.Red)

        val other = TextDrawStyle.from(defaultBrush, 1f)
        assertThat(current.merge(other).brush).isEqualTo(defaultBrush)
    }

    @Test
    fun `merging color with color returns color`() {
        val current = TextDrawStyle.from(Color.Blue.copy(alpha = 0.7f))

        val other = TextDrawStyle.from(Color.Red.copy(alpha = 0.5f))
        assertThat(current.merge(other).color).isEqualTo(Color.Red.copy(alpha = 0.5f))
        assertThat(current.merge(other).alpha).isWithin(TOLERANCE).of(0.5f)
    }

    @Test
    fun `merging brush with brush returns brush`() {
        val current = TextDrawStyle.from(defaultBrush, 0.7f)

        val newBrush = Brush.linearGradient(listOf(Color.White, Color.Black))
        val other = TextDrawStyle.from(newBrush, Float.NaN)
        assertThat(current.merge(other).brush).isEqualTo(TextDrawStyle.from(newBrush, 0.7f).brush)
        assertThat(current.merge(other).alpha).isWithin(TOLERANCE).of(0.7f)
    }

    @Test
    fun `lerps colors if both ends are not brush`() {
        val start = TextDrawStyle.Unspecified
        val stop = TextDrawStyle.from(Color.Red)

        assertThat(lerp(start, stop, fraction = 0.4f)).isEqualTo(
            TextDrawStyle.from(lerp(Color.Unspecified, Color.Red, 0.4f))
        )
    }

    @Test
    fun `lerps discrete if one end is brush and the other end is color`() {
        val start = TextDrawStyle.from(defaultBrush, 1f)
        val stop = TextDrawStyle.from(Color.Red)

        assertThat(lerp(start, stop, fraction = 0.4f)).isEqualTo(
            lerpDiscrete(start, stop, 0.4f)
        )
    }

    @Test
    fun `lerps alpha if both ends are brush`() {
        val start = TextDrawStyle.from(defaultBrush, 0.4f)
        val newBrush = Brush.linearGradient(listOf(Color.White, Color.Black))
        val stop = TextDrawStyle.from(newBrush, 0.7f)

        assertThat(lerp(start, stop, fraction = 0.6f)).isEqualTo(
            TextDrawStyle.from(
                lerpDiscrete(defaultBrush, newBrush, 0.6f),
                lerp(0.4f, 0.7f, 0.6f)
            )
        )
    }
}