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

package androidx.compose.ui.graphics

internal fun BlendMode.toSkija() = when (this) {
    BlendMode.Clear -> org.jetbrains.skija.BlendMode.CLEAR
    BlendMode.Src -> org.jetbrains.skija.BlendMode.SRC
    BlendMode.Dst -> org.jetbrains.skija.BlendMode.DST
    BlendMode.SrcOver -> org.jetbrains.skija.BlendMode.SRC_OVER
    BlendMode.DstOver -> org.jetbrains.skija.BlendMode.DST_OVER
    BlendMode.SrcIn -> org.jetbrains.skija.BlendMode.SRC_IN
    BlendMode.DstIn -> org.jetbrains.skija.BlendMode.DST_IN
    BlendMode.SrcOut -> org.jetbrains.skija.BlendMode.SRC_OUT
    BlendMode.DstOut -> org.jetbrains.skija.BlendMode.DST_OUT
    BlendMode.SrcAtop -> org.jetbrains.skija.BlendMode.SRC_ATOP
    BlendMode.DstAtop -> org.jetbrains.skija.BlendMode.DST_ATOP
    BlendMode.Xor -> org.jetbrains.skija.BlendMode.XOR
    BlendMode.Plus -> org.jetbrains.skija.BlendMode.PLUS
    BlendMode.Modulate -> org.jetbrains.skija.BlendMode.MODULATE
    BlendMode.Screen -> org.jetbrains.skija.BlendMode.SCREEN
    BlendMode.Overlay -> org.jetbrains.skija.BlendMode.OVERLAY
    BlendMode.Darken -> org.jetbrains.skija.BlendMode.DARKEN
    BlendMode.Lighten -> org.jetbrains.skija.BlendMode.LIGHTEN
    BlendMode.ColorDodge -> org.jetbrains.skija.BlendMode.COLOR_DODGE
    BlendMode.ColorBurn -> org.jetbrains.skija.BlendMode.COLOR_BURN
    BlendMode.Hardlight -> org.jetbrains.skija.BlendMode.HARD_LIGHT
    BlendMode.Softlight -> org.jetbrains.skija.BlendMode.SOFT_LIGHT
    BlendMode.Difference -> org.jetbrains.skija.BlendMode.DIFFERENCE
    BlendMode.Exclusion -> org.jetbrains.skija.BlendMode.EXCLUSION
    BlendMode.Multiply -> org.jetbrains.skija.BlendMode.MULTIPLY
    BlendMode.Hue -> org.jetbrains.skija.BlendMode.HUE
    BlendMode.Saturation -> org.jetbrains.skija.BlendMode.SATURATION
    BlendMode.Color -> org.jetbrains.skija.BlendMode.COLOR
    BlendMode.Luminosity -> org.jetbrains.skija.BlendMode.LUMINOSITY
}