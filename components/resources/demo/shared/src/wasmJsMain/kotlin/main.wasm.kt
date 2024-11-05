import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.CanvasBasedWindow
import components.resources.demo.shared.generated.resources.NotoColorEmoji
import components.resources.demo.shared.generated.resources.Res
import components.resources.demo.shared.generated.resources.Workbench_Regular
import components.resources.demo.shared.generated.resources.font_awesome
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.demo.shared.UseResources
import org.jetbrains.compose.resources.preloadAndCacheFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    configureWebResources {
        // Not necessary - It's the same as the default. We add it here just to present this feature.
        resourcePathMapping { path -> "./$path" }
    }
    CanvasBasedWindow("Resources demo + K/Wasm") {
        val font1 by preloadAndCacheFont(Res.font.Workbench_Regular)
        val font2 by preloadAndCacheFont(Res.font.font_awesome, FontWeight.Normal, FontStyle.Normal)
        val emojiFont by preloadAndCacheFont(Res.font.NotoColorEmoji)
        var fontsFallbackInitialiazed by remember { mutableStateOf(false) }

        if (font1 != null && font2 != null && emojiFont != null && fontsFallbackInitialiazed) {
            println("Fonts are ready")
            UseResources()
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            println("Fonts are not ready yet")
        }

        val fontFamilyResolver = LocalFontFamilyResolver.current
        LaunchedEffect(fontFamilyResolver, emojiFont) {
            if (emojiFont != null) {
                // we have an emoji on Strings tab
                fontFamilyResolver.preload(FontFamily(listOf(emojiFont!!)))
                fontsFallbackInitialiazed = true
            }
        }
    }
}