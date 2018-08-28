package com.google.r4a.examples.explorerapp.common.adapters

import com.google.r4a.adapters.*
import com.makeramen.roundedimageview.RoundedImageView

fun RoundedImageView.setCornerRadius(cornerRadius: Dimension) = setCornerRadius(cornerRadius.toFloatPixels(metrics))
fun RoundedImageView.setBorderWidth(borderWidth: Dimension) = setBorderWidth(borderWidth.toFloatPixels(metrics))