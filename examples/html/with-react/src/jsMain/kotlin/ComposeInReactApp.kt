import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import emotion.react.css
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.awaitCancellation
import react.*
import react.dom.client.createRoot
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.input
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.px
import web.html.HTMLDivElement
import web.html.HTMLElement
import web.html.InputType
import web.html.range

@Composable
private fun ComposableComponentToUseInReact(count: State<Int>) {
    repeat(count.value) {
        Div {
            Text("Item $it")
        }
    }
}

/**
 * @param containerRef - [RefObject] - reference to the HTMLElement that is used as a root for Composition
 * @param stateInitialValue - initial state value for the Composition
 * @param stateValueProvider - a lambda that's used to change the state's value
 * @param composable - the content controlled by Compose and mounted in a root provided by [containerRef]
 */
private fun <T> useCompose(
    containerRef: RefObject<out HTMLElement>,
    stateInitialValue: T,
    stateValueProvider: () -> T,
    composable: @Composable (state: State<T>) -> Unit
) {
    val mutableState = useRef(mutableStateOf(stateInitialValue))

    useEffect {
        mutableState.current?.value = stateValueProvider()
    }

    useLayoutEffectOnce {
        val composition = renderComposable(containerRef.current!!.unsafeCast<org.w3c.dom.HTMLElement>()) {
            composable(mutableState.current!!)
        }
        try {
            awaitCancellation()
        } finally {
            composition.dispose()
        }
    }
}

private external interface ListProps : Props {
    var countOfItems: Int
}

private val composeListComponentWrapper = FC<ListProps> { props ->
    val containerRef = useRef<HTMLDivElement>(null)

    useCompose(
        containerRef = containerRef,
        stateInitialValue = 0,
        stateValueProvider = { props.countOfItems }
    ) {
        ComposableComponentToUseInReact(it)
    }

    // This div will be a root for the Composition managed by Compose
    div { ref = containerRef }
}

private val column = FC {
    val (counter, setCounter) = useState(0)

    div {
        css {
            padding = 25.px
        }

        h3 {
            +"Update items count using slider:"
        }

        input {
            type = InputType.range
            value = "$counter"
            onChange = {
                setCounter(it.target.value.toInt())
            }
        }

        h3 {
            +"Compose controlled items:"
        }

        composeListComponentWrapper {
            countOfItems = counter
        }
    }
}

private val appContent = FC {
    val (columnsCount, setColumnsCount) = useState(3)

    a {
        href = "${window.location.origin}?app=composeApp"
        +"GO TO REACT IN COMPOSE EXAMPLE"
    }

    button {
        onClick = {
            setColumnsCount(columnsCount - 1)
        }
        +"Remove column"
    }

    button {
        onClick = {
            setColumnsCount(columnsCount + 1)
        }
        +"Add column"
    }

    div {
        css {
            display = Display.flex
            flexDirection = FlexDirection.row
        }

        repeat(columnsCount) {
            column()
        }
    }
}

fun composeInReactAppExample() {
    createRoot(document.getElementById("root")!!.unsafeCast<web.dom.Element>())
        .render(appContent.create())
}
