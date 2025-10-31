package org.company.app

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import multiplatform_app.sharedui.generated.resources.Res
import multiplatform_app.sharedui.generated.resources.cyclone
import org.jetbrains.compose.resources.stringResource

@Composable
fun App() {
    BasicText(text = stringResource(Res.string.cyclone))
}
