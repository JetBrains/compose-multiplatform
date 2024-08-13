package me.sample.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import kmpresourcepublication.featuremodule.generated.resources.*

@Composable
fun MyFeatureText(modifier: Modifier = Modifier, txt: String) {
    Text(txt + stringResource(Res.string.str_1), modifier)
}