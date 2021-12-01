import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.*
import org.jetbrains.skia.FramebufferFormat.Companion.GR_GL_RGBA8
import org.jetbrains.skiko.FrameDispatcher
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.system.exitProcess

fun main() {
    var width = 640
    var height = 480

    glfwInit()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    val windowHandle: Long = glfwCreateWindow(width, height, "Compose LWJGL Demo", NULL, NULL)
    glfwMakeContextCurrent(windowHandle)
    glfwSwapInterval(1)

    GL.createCapabilities()

    val context = DirectContext.makeGL()
    var surface = createSurface(width, height, context) // Skia Surface, bound to the OpenGL framebuffer
    val glfwDispatcher = GlfwCoroutineDispatcher() // a custom coroutine dispatcher, in which Compose will run

    glfwSetWindowCloseCallback(windowHandle) {
        glfwDispatcher.stop()
    }

    lateinit var composeScene: ComposeScene

    fun render() {
        surface.canvas.clear(Color.WHITE)
        composeScene.constraints = Constraints(maxWidth = width, maxHeight = height)
        composeScene.render(surface.canvas, System.nanoTime())

        context.flush()
        glfwSwapBuffers(windowHandle)
    }

    val frameDispatcher = FrameDispatcher(glfwDispatcher) { render() }

    val density = Density(glfwGetWindowContentScale(windowHandle))
    composeScene = ComposeScene(glfwDispatcher, density, invalidate = frameDispatcher::scheduleFrame)

    glfwSetWindowSizeCallback(windowHandle) { _, windowWidth, windowHeight ->
        width = windowWidth
        height = windowHeight
        surface.close()
        surface = createSurface(width, height, context)

        glfwSwapInterval(0)
        render()
        glfwSwapInterval(1)
    }

    composeScene.subscribeToGLFWEvents(windowHandle)
    composeScene.setContent { App() }
    glfwShowWindow(windowHandle)

    glfwDispatcher.runLoop()

    composeScene.close()
    glfwDestroyWindow(windowHandle)

    exitProcess(0)
}

private fun createSurface(width: Int, height: Int, context: DirectContext): Surface {
    val fbId = GL11.glGetInteger(GL_FRAMEBUFFER_BINDING)
    val renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, fbId, GR_GL_RGBA8)
    return Surface.makeFromBackendRenderTarget(
        context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB
    )
}

private fun glfwGetWindowContentScale(window: Long): Float {
    val array = FloatArray(1)
    glfwGetWindowContentScale(window, array, FloatArray(1))
    return array[0]
}