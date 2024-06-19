/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

@Composable
fun MainView() {
    UseResources()
}

@Preview(showBackground = true)
@Composable
fun ImagesResPreview() {
    ImagesRes(PaddingValues())
}

@OptIn(ExperimentalResourceApi::class)
@Preview(showBackground = true)
@Composable
fun FileResPreview() {
    PreviewContextConfigurationEffect()
    FileRes(PaddingValues())
}
