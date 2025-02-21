import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import config.PurpleGrey40
import config.UIScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class, ExperimentalFoundationApi::class)
@Composable
fun App() = UIScreen {

    val count = 7
    val pagerState = rememberPagerState(pageCount = { count })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.padding(bottom = 76.dp).fillMaxSize()) {
            HorizontalPager(state = pagerState) { page ->
                // Our page content
                Box(
                    modifier = Modifier.padding(10.dp).fillMaxSize().background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                ) {
                    Text(
                        text = "Page : ${page + 1}",
                        fontSize = TextUnit(50f, TextUnitType.Sp),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().align(alignment = Alignment.Center)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(alignment = Alignment.BottomCenter).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                onClick = {
                    val position =
                        if (pagerState.currentPage != 0) pagerState.currentPage - 1 else count - 1
                    coroutineScope.launch {
                        // Call scroll to on pagerState
                        pagerState.animateScrollToPage(position)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp).weight(3f)
            ) {
                Icon(
                    painter = painterResource("ic_arrow_back_24.xml"),
                    contentDescription = ""
                )
            }

            Indicators(
                count = count,
                size = 10,
                spacer = 5,
                selectedColor = Color.Red,
                unselectedColor = PurpleGrey40,
                selectedIndex = pagerState.currentPage,
                selectedLength = 50,
                modifier = Modifier.weight(4f)
            )

            Button(
                onClick = {
                    val position =
                        if (pagerState.currentPage != (count - 1)) pagerState.currentPage + 1 else 0
                    coroutineScope.launch {
                        // Call scroll to on pagerState
                        pagerState.animateScrollToPage(position)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp).weight(3f)
            ) {
                Icon(
                    painter = painterResource("ic_arrow_forward_24.xml"),
                    contentDescription = ""
                )
            }
        }
    }
}