/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.demo.shared

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
fun MainView() {
    UseResources()
}

@Preview
@Composable
fun MainViewPreview() {
    MainView()
}

@Preview
@Composable
fun ImagesResPreview() {
    ImagesRes(PaddingValues())
}

@Preview
@Composable
fun FileResPreview() {
    FileRes(PaddingValues())
}
