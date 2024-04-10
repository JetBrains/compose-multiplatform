package me.sample.app

import androidx.compose.runtime.Composable
import kmpresourcepublication.appmodule.generated.resources.Res
import kmpresourcepublication.appmodule.generated.resources.iOS_str
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun getPlatformSpecificString(): String =
    stringResource(Res.string.iOS_str)