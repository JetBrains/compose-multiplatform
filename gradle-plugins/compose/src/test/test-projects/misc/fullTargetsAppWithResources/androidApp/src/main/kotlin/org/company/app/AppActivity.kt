package org.company.app

import androidx.compose.runtime.Composable
import multiplatform_app.sharedui.generated.resources.Res
import multiplatform_app.sharedui.generated.resources.cyclone
import org.jetbrains.compose.resources.stringResource


@Composable
fun Foo() {
    val s = stringResource(Res.string.cyclone)
    App()
}
