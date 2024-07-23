package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val emptyFontBase64 =
    "T1RUTwAJAIAAAwAQQ0ZGIML7MfIAAAQIAAAA2U9TLzJmMV8PAAABAAAAAGBjbWFwANUAVwAAA6QAAABEaGVhZCMuU7" +
            "IAAACcAAAANmhoZWECvgAmAAAA1AAAACRobXR4Az4AAAAABOQAAAAQbWF4cAAEUAAAAAD4AAAABm5hbWUpw3nbAAABYAAAAkNwb3N0AAMA" +
            "AAAAA+gAAAAgAAEAAAABAADs7nftXw889QADA+gAAAAA4WWJaQAAAADhZYlpAAAAAAFNAAAAAAADAAIAAAAAAAAAAQAAArz+1AAAAU0AAA" +
            "AAAAAAAQAAAAAAAAAAAAAAAAAAAAQAAFAAAAQAAAADAHwB9AAFAAACigK7AAAAjAKKArsAAAHfADEBAgAAAAAAAAAAAAAAAAAAAAEAAAAA" +
            "AAAAAAAAAABYWFhYAEAAIABfArz+1AAAAAAAAAAAAAEAAAAAAV4AAAAgACAAAAAAACIBngABAAAAAAAAAAIAbwABAAAAAAABAAUAAAABAA" +
            "AAAAACAAcADwABAAAAAAADABAAdQABAAAAAAAEAA0AJAABAAAAAAAFAAIAbwABAAAAAAAGAAwASwABAAAAAAAHAAIAbwABAAAAAAAIAAIA" +
            "bwABAAAAAAAJAAIAbwABAAAAAAAKAAIAbwABAAAAAAALAAIAbwABAAAAAAAMAAIAbwABAAAAAAANAAIAbwABAAAAAAAOAAIAbwABAAAAAA" +
            "AQAAUAAAABAAAAAAARAAcADwADAAEECQAAAAQAcQADAAEECQABAAoABQADAAEECQACAA4AFgADAAEECQADACAAhQADAAEECQAEABoAMQAD" +
            "AAEECQAFAAQAcQADAAEECQAGABgAVwADAAEECQAHAAQAcQADAAEECQAIAAQAcQADAAEECQAJAAQAcQADAAEECQAKAAQAcQADAAEECQALAA" +
            "QAcQADAAEECQAMAAQAcQADAAEECQANAAQAcQADAAEECQAOAAQAcQADAAEECQAQAAoABQADAAEECQARAA4AFmVtcHR5AGUAbQBwAHQAeVJl" +
            "Z3VsYXIAUgBlAGcAdQBsAGEAcmVtcHR5IFJlZ3VsYXIAZQBtAHAAdAB5ACAAUgBlAGcAdQBsAGEAcmVtcHR5UmVndWxhcgBlAG0AcAB0AH" +
            "kAUgBlAGcAdQBsAGEAciIiACIAIiIiOmVtcHR5IFJlZ3VsYXIAIgAiADoAZQBtAHAAdAB5ACAAUgBlAGcAdQBsAGEAcgAAAAABAAMAAQAA" +
            "AAwABAA4AAAACgAIAAIAAgAAACAAQQBf//8AAAAAACAAQQBf//8AAP/h/8H/pAABAAAAAAAAAAAAAAADAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAQAEAQABAQENZW1wdHlSZWd1bGFyAAEBASf4GwD4HAL4HQP4HgSLi/lQ9+EFHQAAAHgPHQAAAH8Rix0AAADZEgAHAQED" +
            "EBUcISIsIiJlbXB0eSBSZWd1bGFyZW1wdHlSZWd1bGFyc3BhY2VBdW5kZXJzY29yZQAAAAGLAYwBjQAEAQFMT1FT+F2f+TcVi4uL/TeLiw" +
            "iLi/g1i4uLCIuLi/k3i4sIi4v8NYuLiwi7/QcVi4uL+NeLiwiLi/fUi4uLCIuLi/zXi4sIi4v71IuLiwgO9+EOnw6fDgAAAAHJAAABTQAA" +
            "ABQAAAAUAAA="

@OptIn(ExperimentalEncodingApi::class)
private val defaultEmptyFont by lazy { Font("org.jetbrains.compose.emptyFont", Base64.decode(emptyFontBase64)) }

private val fontCache = AsyncCache<String, Font>()

@Composable
actual fun Font(resource: FontResource, weight: FontWeight, style: FontStyle): Font {
    val resourceReader = LocalResourceReader.currentOrPreview
    val fontFile by rememberResourceState(resource, weight, style, { defaultEmptyFont }) { env ->
        val path = resource.getResourceItemByEnvironment(env).path
        val key = "$path:$weight:$style"
        fontCache.getOrLoad(key) {
            val fontBytes = resourceReader.read(path)
            Font(path, fontBytes, weight, style)
        }
    }
    return fontFile
}