// READ ME FIRST!
//
// Code in this file is shared between the Android and Desktop JVM targets.
// Kotlin's hierarchical multiplatform projects currently
// don't support sharing code depending on JVM declarations.
//
// You can follow the progress for HMPP JVM & Android intermediate source sets here:
// https://youtrack.jetbrains.com/issue/KT-42466
//
// The workaround used here to access JVM libraries causes IntelliJ IDEA to not
// resolve symbols in this file properly.
//
// Resolution errors in your IDE do not indicate a problem with your setup.

package androidx.ui.examples.jetissues.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.examples.jetissues.data.*
import androidx.ui.examples.jetissues.query.IssueQuery
import androidx.ui.examples.jetissues.query.IssuesQuery
import androidx.ui.examples.jetissues.query.type.OrderDirection
import androidx.ui.examples.jetissues.view.common.VerticalScrollbar
import kotlinx.coroutines.runBlocking
import org.ocpsoft.prettytime.PrettyTime
import java.lang.Integer.parseInt
import java.util.*

val Repository = compositionLocalOf<IssuesRepository> { error("Undefined repository") }

@Composable
fun JetIssuesView() {
    MaterialTheme(
        colors = lightThemeColors
    ) {
        Main()
    }

}

@Composable
fun Main() {
    val currentIssue: MutableState<IssuesQuery.Node?> = remember { mutableStateOf(null) }
    BoxWithConstraints {
       if (maxWidth.value > 1000) {
           TwoColumnsLayout(currentIssue)
       } else {
           SingleColumnLayout(currentIssue)
       }
    }

}

@Composable
fun SingleColumnLayout(currentIssue: MutableState<IssuesQuery.Node?>) {
    val issue = currentIssue.value
    if(issue == null) {
        IssuesList(currentIssue)
    } else {
        Column {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "#${issue.number}",
                                style = MaterialTheme.typography.h5
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    currentIssue.value = null
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                },
                content = {
                    CurrentIssue(currentIssue.value)
                }
            )
        }
    }
}

@Composable
fun TwoColumnsLayout(currentIssue: MutableState<IssuesQuery.Node?>) {
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(0.4f), contentAlignment = Alignment.Center) {
            IssuesList(currentIssue)
        }
        CurrentIssue(currentIssue.value)
    }
}

@Composable
fun CurrentIssue(
    issue: IssuesQuery.Node?
) {
    when (issue) {
        null -> CurrentIssueStatus { Text("Select issue") }
        else -> {
            val repo = Repository.current
            val issueBody = uiStateFrom(issue.number) { clb: (Result<IssueQuery.Issue>) -> Unit ->
                repo.getIssue(issue.number, callback = clb)
            }.value
            when (issueBody) {
                is UiState.Loading -> CurrentIssueStatus { Loader() }
                is UiState.Error -> CurrentIssueStatus { Error("Issue loading error") }
                is UiState.Success -> CurrentIssueActive(issue, issueBody.data)
            }
        }
    }
}

@Composable
fun CurrentIssueStatus(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
fun CurrentIssueActive(issue: IssuesQuery.Node, body: IssueQuery.Issue) {
    Box(Modifier.fillMaxSize()) {
        val state = rememberScrollState()

        Column(modifier = Modifier.padding(15.dp).fillMaxSize().verticalScroll(state)) {
            SelectionContainer {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.h5
                )
            }

            Row(horizontalArrangement = Arrangement.Center) {
                CreatedBy(issue)
            }

            Labels(issue.labels)

            Spacer(Modifier.height(8.dp))

            SelectionContainer {
                Text(
                    text = body.body,
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.body1
                )
            }
        }

        VerticalScrollbar(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            state
        )
    }
}

@Composable
fun IssuesList(currentIssue: MutableState<IssuesQuery.Node?>) {
    val scroll = rememberScrollState()
    val issuesState = remember { mutableStateOf(IssuesState.OPEN) }
    val issuesOrder = remember { mutableStateOf(OrderDirection.DESC) }
    Column {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "JetIssues") },
                    actions = {
                        OrderButton(issuesOrder, scroll)
                    }
                )
            },
            content = {
                Column {
                    FilterTabs(issuesState, scroll)
                    ListBody(
                        scroll,
                        currentIssue = currentIssue,
                        issuesState = issuesState.value,
                        issuesOrder = issuesOrder.value
                    )
                }
            }
        )
    }
}

@Composable
fun OrderButton(order: MutableState<OrderDirection>, scroll: ScrollState) {
    when (order.value) {
        OrderDirection.DESC ->
            Button(onClick = {
                order.value = OrderDirection.ASC
                runBlocking {
                    scroll.scrollTo(0)
                }
            }) {
                Text("ASC")
            }
        OrderDirection.ASC ->
            Button(onClick = {
                order.value = OrderDirection.DESC
                runBlocking {
                    scroll.scrollTo(0)
                }
            }) {
                Text("DESC")
            }
        else -> Error("Unknown direction")
    }
}


@Composable
fun FilterTabs(issuesState: MutableState<IssuesState>, scroll: ScrollState) {
    TabRow(selectedTabIndex = IssuesState.values().toList().indexOf(issuesState.value)) {
        IssuesState.values().forEach {
            Tab(
                text = { Text(it.title) },
                selected = issuesState.value == it,
                onClick = {
                    issuesState.value = it
                    runBlocking {
                        scroll.scrollTo(0)
                    }
                }
            )
        }
    }
}

@Composable
fun ListBody(
    scroll: ScrollState,
    currentIssue: MutableState<IssuesQuery.Node?>,
    issuesState: IssuesState,
    issuesOrder: OrderDirection
) {
    val repo = Repository.current
    val issues = uiStateFrom(issuesState, issuesOrder) { clb: (Result<Issues>) -> Unit ->
        repo.getIssues(issuesState, issuesOrder, callback = clb)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(scroll)) {
            issues.value.let {
                when (it) {
                    is UiState.Success -> {
                        for (iss in it.data.nodes) {
                            Box(modifier = Modifier.clickable {
                                currentIssue.value = iss
                            }, contentAlignment = Alignment.CenterStart) {
                                ListItem(iss)
                            }
                        }
                        MoreButton(issues)
                    }

                    is UiState.Loading -> Loader()
                    is UiState.Error -> Error("Issues loading error")
                }
            }
        }
        VerticalScrollbar(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            scroll
        )
    }

}

@Composable
fun ListItem(x: IssuesQuery.Node) {
    Card(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
        CardBody(x)
    }
}

@Composable
fun CardBody(x: IssuesQuery.Node) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row {
                CreatedAt(x)
                Spacer(Modifier.width(10.dp))
                Number(x)
            }

            Title(x)
            Labels(x.labels)
        }
    }
}

@Composable
fun Title(x: IssuesQuery.Node) {
    Text(text = x.title)
}

private val timePrinter = PrettyTime()

private val ISSUE_DATE_STYLE = TextStyle(color = Color.Gray, fontStyle = FontStyle.Italic)

@Composable
fun CreatedAt(x: IssuesQuery.Node) {
    Text(text = timePrinter.format(x.createdAt as Date), style = ISSUE_DATE_STYLE)
}

@Composable
fun Number(x: IssuesQuery.Node) {
    Text(text = "#${x.number}")
}

@Composable
fun CreatedBy(issue: IssuesQuery.Node) {
    val text = AnnotatedString.Builder().apply {
        pushStyle(ISSUE_DATE_STYLE.toSpanStyle())
        append(timePrinter.format(issue.createdAt as Date))
        pop()
        issue.author?.login?.let {
            append(" by ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(it)
        }
    }.toAnnotatedString()
    Text(text = text)
}

@Composable
fun MoreButton(issues: MutableState<UiState<Issues>>) {
    val value = issues.value
    if (value !is  UiState.Success) {
        return
    }
    val issuesData = value.data
    val cursor = issuesData.cursor
    if (cursor == null) {
        return
    }

    var loading by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(10.dp)
    ) {
        if (loading) {
            Loader()
        } else {
            val repo = Repository.current
            Button(onClick = {
                loading = true
                repo.getIssues(issuesData.state, issuesData.order, cursor) {
                    loading = false
                    when (it) {
                        is Result.Error -> issues.value = UiState.Error(it.exception)
                        is Result.Success -> issues.value = UiState.Success(it.data.copy(nodes = issuesData.nodes + it.data.nodes))
                    }
                }
            }) {
                Text(text = "More")
            }
        }
    }
}


@Composable
fun Labels(labels: IssuesQuery.Labels?) {
    Row {
        labels?.nodes?.filterNotNull()?.forEach {
            val color = parseColor(it.color)
            val textColor = if (color.luminance() > 0.5) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .background(color = color)
                    .clip(shape = RoundedCornerShape(3.dp))
            ) {
                Text(
                    text = it.name,
                    modifier = Modifier.padding(3.dp),
                    style = TextStyle(color = textColor)
                )
            }
        }
    }
}

@Composable
fun Loader() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun Error(err: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        Text(text = err, style = TextStyle(color = MaterialTheme.colors.error, fontWeight = FontWeight.Bold))
    }
}

val lightThemeColors = lightColors(
    primary = Color(0xFFDD0D3C),
    primaryVariant = Color(0xFFC20029),
    secondary = Color.White,
    error = Color(0xFFD00036)
)

fun parseColor(hexString: String): Color {
    val red = parseInt(hexString.subSequence(0, 2).toString(), 16)
    val green = parseInt(hexString.subSequence(2, 4).toString(), 16)
    val blue = parseInt(hexString.subSequence(4, 6).toString(), 16)
    return Color(red, green, blue)
}
