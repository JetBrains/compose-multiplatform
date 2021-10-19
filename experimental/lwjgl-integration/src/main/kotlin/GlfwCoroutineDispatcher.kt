import kotlinx.coroutines.CoroutineDispatcher
import org.lwjgl.glfw.GLFW
import kotlin.coroutines.CoroutineContext

class GlfwCoroutineDispatcher : CoroutineDispatcher() {
    private val tasks = mutableListOf<Runnable>()
    private val tasksCopy = mutableListOf<Runnable>()
    private var isStopped = false

    fun runLoop() {
        while (!isStopped) {
            synchronized(tasks) {
                tasksCopy.addAll(tasks)
                tasks.clear()
            }
            for (runnable in tasksCopy) {
                if (!isStopped) {
                    runnable.run()
                }
            }
            tasksCopy.clear()
            GLFW.glfwWaitEvents()
        }
    }

    fun stop() {
        isStopped = true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasks) {
            tasks.add(block)
        }
        GLFW.glfwPostEmptyEvent()
    }
}