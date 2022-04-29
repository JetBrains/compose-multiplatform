import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

val UIDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

abstract class ExternalComponent {
    abstract fun render()
    open fun dispose() = Unit
}

open class ExternalContainer : ExternalComponent() {
    val children = mutableListOf<ExternalComponent>()

    override fun dispose() {
        children.forEach {
            it.dispose()
        }
    }

    override fun render() {
        children.forEach {
            it.render()
        }
    }
}

open class ExternalTextField : ExternalComponent() {
    var onUserInput: ((String) -> Unit)? = null
    var text = ""

    override fun render() {
        println("TextField $text")
    }

    fun userInput(text: String) {
        this.text = text
        onUserInput?.invoke(text)
    }
}
