package me.sample.app

import androidx.compose.runtime.Composable
import kmpresourcepublication.appmodule.generated.resources.Res
import kmpresourcepublication.appmodule.generated.resources.wasm_str
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun getPlatformSpecificString(): String =
    stringResource(Res.string.wasm_str)