import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.renderComposeScene
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.*

object Main {
    @JvmStatic
    @OptIn(ExperimentalComposeUiApi::class)
    fun main(args: Array<String>) {
        val workingDir = args.getOrNull(0)?.let { File(it) }
        workingDir?.mkdirs()
        if (workingDir == null || !workingDir.isDirectory) {
            error("Working directory must be passes as the first argument. '$workingDir' is not a directory")
        }

        val image = renderComposeScene(height = 10, width = 10) {
            mainShape()
        }
        val encodedImage = image.encodeToData(EncodedImageFormat.PNG) ?: error("Could not encode image as png")
        workingDir.resolve("main-image.actual.png").writeBytes(encodedImage.bytes)

        val mainMethods = this.javaClass.declaredMethods
            .mapTo(TreeSet()) { it.name }
            .joinToString("\n")
        workingDir.resolve("main-methods.actual.txt").writeText(mainMethods)

        serializer()
    }

    @Composable
    fun mainShape() {
        triangle(Color.Magenta)
    }

    @Composable
    fun unused() {
        transitivelyUnused()
    }

    @Composable
    fun transitivelyUnused() {
        triangle(Color.Gray)
    }

    @Composable
    fun keptByKeepRule() {
        fillShape(Color.Blue, CircleShape)
    }

    // https://youtrack.jetbrains.com/issue/CMP-8050/Desktop-runRelease-crash-when-upgrade-to-CMP-1.8.0-rc01#focus=Comments-27-11963863.0-0
    @OptIn(InternalSerializationApi::class)
    fun serializer() {
        LoginRoute::class.serializer()
        LoginRoute.serializer()
    }
}

@Composable
fun triangle(color: Color) {
    fillShape(color, GenericShape { size, _ ->
        moveTo(size.width / 2f, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
    })
}

@Composable
fun fillShape(color: Color, shape: Shape){
    Column(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)) {
        Box(
            modifier = Modifier.clip(shape).fillMaxSize().background(color)
        )
    }
}

@Serializable
data class LoginRoute(val id: Long? = null)