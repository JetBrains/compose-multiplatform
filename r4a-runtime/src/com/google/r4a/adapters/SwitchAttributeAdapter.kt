@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.Switch

fun Switch.setSwitchMinWidth(switchMinWidth: Dimension) = setSwitchMinWidth(switchMinWidth.toIntPixels(metrics))
fun Switch.setSwitchPadding(switchPadding: Dimension) = setSwitchPadding(switchPadding.toIntPixels(metrics))
fun Switch.setThumbTextPadding(thumbTextPadding: Dimension) = setThumbTextPadding(thumbTextPadding.toIntPixels(metrics))