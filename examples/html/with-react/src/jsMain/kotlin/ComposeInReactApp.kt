import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onInputFunction
import org.w3c.dom.HTMLElement
import react.*
import react.dom.*
import styled.css
import styled.styledDiv

@Composable
private fun ComposableComponentToUseInReact(count: State<Int>) {
    repeat(count.value) {
        Div {
            Text("Item $it")
        }
    }
}

/**
 * @param containerRef - [RMutableRef] - reference to the HTMLElement that is used as a root for Composition
 * @param stateInitialValue - initial state value for the Composition
 * @param stateValueProvider - a lambda that's used to change the state's value
 * @param composable - the content controlled by Compose and mounted in a root provided by [containerRef]
 */
private fun <T> useCompose(
    containerRef: RMutableRef<HTMLElement>,
    stateInitialValue: T,
    stateValueProvider: () -> T,
    composable: @Composable (state: State<T>) -> Unit
) {
    val mutableState = useRef(mutableStateOf(stateInitialValue))

    useEffect {
        mutableState.current?.value = stateValueProvider()
    }

    useLayoutEffectWithCleanup(dependencies = emptyList()) {
        val composition = renderComposable(containerRef.current!!) {
            composable(mutableState.current!!)
        }
        return@useLayoutEffectWithCleanup {
            composition.dispose()
        }
    }
}

private external interface ListProps : RProps {
    var countOfItems: Int
}

private val composeListComponentWrapper = functionalComponent<ListProps> { props ->
    val containerRef = useRef<HTMLElement>(null)

    useCompose(
        containerRef = containerRef,
        stateInitialValue = 0,
        stateValueProvider = { props.countOfItems }
    ) {
        ComposableComponentToUseInReact(it)
    }

    // This div will be a root for the Composition managed by Compose
    div { ref { containerRef.current = it } }
}

private val column = functionalComponent<RProps> {
    val (counter, setCounter) = useState(0)

    styledDiv {
        css {
            padding = "25px"
        }

        h3 {
            +"Update items count using slider:"
        }

        input(type = InputType.range) {
            attrs {
                onInputFunction = {
                    setCounter(it.target?.asDynamic().value.toString().toInt())
                }
                value = "$counter"
            }
        }

        h3 {
            +"Compose controlled items:"
        }

        child(composeListComponentWrapper) {
            this.attrs {
                countOfItems = counter
            }
        }
    }
}

private val appContent = functionalComponent<RProps> {
    val (columnsCount, setColumnsCount) = useState(3)

    a(href = "${window.location.origin}?app=composeApp") {
        +"GO TO REACT IN COMPOSE EXAMPLE"
    }

    button {
        attrs {
            onClickFunction = {
                setColumnsCount(columnsCount - 1)
            }
        }
        +"Remove column"
    }

    button {
        attrs {
            onClickFunction = {
                setColumnsCount(columnsCount + 1)
            }
        }
        +"Add column"
    }

    styledDiv {
        css {
            display = Display.flex
            flexDirection = FlexDirection.row
        }

        repeat(columnsCount) {
            child(column)
        }
    }
}

fun composeInReactAppExample() {
    render(document.getElementById("root")) {
        child(appContent)
    }
}