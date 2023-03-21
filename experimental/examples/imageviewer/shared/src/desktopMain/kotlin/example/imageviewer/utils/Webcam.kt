package example.imageviewer.utils

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import java.awt.image.*
import kotlin.math.max
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

private var driverInitialized = false

@Composable
internal fun rememberWebcamListState(): WebcamListState {
    val webcamListState = remember { WebcamListState() }
    webcamListState.setup()

    return webcamListState
}

@Stable
internal class WebcamListState {
    private val webcamsMutable = mutableStateListOf<Webcam>()
    val webcams: List<Webcam> = webcamsMutable

    var isLoading by mutableStateOf(true)
        private set
    var defaultWebcam: Webcam? by mutableStateOf(null) // null when there is no camera.
        private set

    @Composable
    fun setup() {
        LaunchedEffect(Unit) {
            isLoading = true
            withContext(Dispatchers.IO) {
                // check if running on ARM/MacOS and apply the native driver.
                if(driverInitialized.not()) {
                    val os = System.getProperty("os.name")
                    val driver = if(os.contains("mac") || os.contains("darwin")) {
                        NativeDriver()
                    } else {
                        NativeDriver()
                        // TODO another Driver that already works on Windows and Linux
                    }

                    Webcam.setDriver(driver)
                    driverInitialized = true
                }

                Webcam.getWebcams().forEach { webcam ->
                    webcamsMutable.removeIf { it.name == webcam.name }
                    webcamsMutable.add(webcam)
                }

                defaultWebcam = Webcam.getDefault()
            }
            isLoading = false
        }

        DisposableEffect(Unit) {
            val listener = object : WebcamDiscoveryListener {
                override fun webcamFound(event: WebcamDiscoveryEvent) {
                    webcamsMutable.removeIf { it.name == event.webcam.name }
                    webcamsMutable.add(event.webcam)
                }

                override fun webcamGone(event: WebcamDiscoveryEvent) {
                    webcamsMutable.removeIf { it.name == event.webcam.name }
                }
            }
            Webcam.addDiscoveryListener(listener)

            onDispose {
                Webcam.removeDiscoveryListener(listener)
            }
        }
    }
}

@Composable
internal fun rememberWebcamState(webcam: Webcam): WebcamState {
    val state = remember { WebcamState(webcam) }
    state.setup()

    return state
}

@Stable
internal class WebcamState(webcam: Webcam) {
    var webcam by mutableStateOf(webcam)
    var resolutions by mutableStateOf(webcam.viewSizes.toList())
        private set

    var currentWebcamResolution by mutableStateOf(webcam.viewSize)

    var lastFrame by mutableStateOf<ImageBitmap?>(null)
        private set

    var fpsLimitation: Int? by mutableStateOf(30) // if null, it will try the most FPS the computer can take.

    var timeSpentPerFrame: Long by mutableStateOf(0L)
        private set

    @OptIn(ExperimentalTime::class)
    @Composable
    fun setup() {
        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(webcam) {
            coroutineScope.launch(Dispatchers.IO) {
                webcam.lock.disable()
                webcam.open(true)
            }
            onDispose {
                webcam.close()
            }
        }

        LaunchedEffect(webcam) {
            resolutions = webcam.viewSizes.toList()
        }

        LaunchedEffect(currentWebcamResolution) {
            requiredClose(webcam) {
                viewSize = currentWebcamResolution
            }
        }

        LaunchedEffect(webcam) {
            withContext(Dispatchers.IO) {
                while (true) {
                    val (imageBitmap, measure) = measureTimedValue {
                        webcam.imageBitmap()
                    }
                    if (imageBitmap != null) {
                        lastFrame = imageBitmap
                        timeSpentPerFrame = measure.inWholeMilliseconds

                        if(fpsLimitation != null) {
                            delay(max(fpsLimitation!! - timeSpentPerFrame, 1))
                        }
                    } else {
                        delay(50) // delay until pickup camera again
                    }
                    yield()
                }
            }
        }
    }
}

private fun requiredClose(webcam: Webcam, apply: Webcam.() -> Unit) {
    val listener = object : WebcamListener {
        override fun webcamOpen(we: WebcamEvent) {
            we.source.removeWebcamListener(this)
        }

        override fun webcamClosed(we: WebcamEvent) {
            we.source.apply()
            we.source.open()
        }

        override fun webcamDisposed(we: WebcamEvent?) {}

        override fun webcamImageObtained(we: WebcamEvent?) {}

    }

    webcam.addWebcamListener(listener)
    webcam.close()
}

private fun Webcam.imageBitmap(): ImageBitmap? {
    val buffer = imageBytes ?: return null
    val arrayBuffer = ByteArray(buffer.capacity())


    val width = viewSize.width
    val height = viewSize.height

    val bytesPerPixel = 4
    val pixels = ByteArray(width * height * bytesPerPixel)

    buffer.mark()
    buffer.position(0)
    buffer.get(arrayBuffer, 0, buffer.capacity())
    buffer.reset()

    // d15f35 -> 3558c7ff
    // to not go through BufferedImage.toComposeImageBitmap
    // instead we map directly de sRGB from Webcam Image Buffer
    // this is way faster, probably there is faster approach.
    var k = 0
    for (i in 0 until buffer.capacity() step 3) {
        val r = arrayBuffer.get(i)
        val g = arrayBuffer.get(i + 1)
        val b = arrayBuffer.get(i + 2)
        pixels[k++] = b
        pixels[k++] = g
        pixels[k++] = r
        pixels[k++] = 0xff.toByte()
    }

    val bitmap = Bitmap()

    bitmap.allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(pixels)

    return bitmap.asComposeImageBitmap()
}
