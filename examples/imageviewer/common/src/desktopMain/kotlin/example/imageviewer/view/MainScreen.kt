package example.imageviewer.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.Surface
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageAsset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import example.imageviewer.model.AppState
import example.imageviewer.model.Picture
import example.imageviewer.model.ScreenType
import example.imageviewer.model.ContentState
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.Transparent
import example.imageviewer.style.TranslucentBlack
import example.imageviewer.style.TranslucentWhite
import example.imageviewer.style.PreviewImageAreaHoverColor
import example.imageviewer.style.MiniatureHoverColor
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.LightGray
import example.imageviewer.style.icRefresh
import example.imageviewer.style.icEmpty
import example.imageviewer.style.icDots
import example.imageviewer.utils.toByteArray
import example.imageviewer.ResString
import org.jetbrains.skija.Image
import org.jetbrains.skija.IRect

@Composable
fun setMainScreen(content: ContentState) {

    if (content.isContentReady()) {
        Column {
            setTopContent(content)
            setScrollableArea(content)
        }
    } else {
        setLoadingScreen(content)
    }
}

@Composable
private fun setLoadingScreen(content: ContentState) {

    Box {
        Column {
            setTopContent(content)
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            Surface(color = DarkGray, elevation = 4.dp, shape = CircleShape) {
                CircularProgressIndicator(
                    modifier = Modifier.preferredSize(50.dp).padding(3.dp, 3.dp, 4.dp, 4.dp),
                    color = DarkGreen
                )
            }
        }
        Text(
            text = ResString.loading,
            modifier = Modifier.align(Alignment.Center).offset(0.dp, 70.dp),
            style = MaterialTheme.typography.body1,
            color = Foreground
        )
    }
}

@Composable
fun setTopContent(content: ContentState) {

    setTitleBar(text = "ImageViewer", content = content)
    setPreviewImageUI(content)
    setSpacer(h = 10)
    setDivider()
    setSpacer(h = 5)
}

@Composable
fun setTitleBar(text: String, content: ContentState) {
    val refreshButtonHover = remember { mutableStateOf(false) }
    TopAppBar(
        backgroundColor = DarkGreen,
        title = {
        Row(Modifier.preferredHeight(50.dp)) {
            Text(
                text,
                color = Foreground,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
            )
            Surface(
                color = Transparent,
                modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                Clickable(
                    modifier = Modifier.hover(
                        onEnter = {
                            refreshButtonHover.value = true
                            false
                        },
                        onExit = {
                            refreshButtonHover.value = false
                            false
                        }
                    )
                    .background(color = if (refreshButtonHover.value) TranslucentBlack else Transparent),
                    onClick = {
                        if (content.isContentReady()) {
                            content.refresh()
                        }
                    }
                ) {
                    Image(
                        icRefresh(),
                        modifier = Modifier.preferredSize(35.dp)
                    )
                }
            }
        }
    })
}

@Composable
fun setPreviewImageUI(content: ContentState) {
    Clickable(
        modifier = Modifier.background(color = DarkGray),
        onClick = {
            AppState.screenState(ScreenType.FullscreenImage)
        }
    ) {
        Card(
            backgroundColor = Transparent,
            modifier = Modifier.preferredHeight(250.dp),
            shape = RectangleShape,
            elevation = 1.dp
        ) {
            Image(
                if (content.isMainImageEmpty())
                    icEmpty()
                else Image.makeFromEncoded(
                    toByteArray(content.getSelectedImage())
                ).asImageAsset(),
                modifier = Modifier
                    .fillMaxWidth().padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 5.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun setMiniatureUI(
    picture: Picture,
    content: ContentState
) {
    val cardHover = remember { mutableStateOf(false) }
    val infoButtonHover = remember { mutableStateOf(false) }
    Card(
        backgroundColor = if (cardHover.value) MiniatureHoverColor else MiniatureColor,
        modifier = Modifier.padding(start = 10.dp, end = 18.dp).preferredHeight(70.dp)
            .fillMaxWidth()
            .hover(onEnter = {
                cardHover.value = true
                false
            },
            onExit = {
                cardHover.value = false
                false
            })
            .clickable {
                content.setMainImage(picture)
            },
        shape = RectangleShape
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Clickable(
                onClick = {
                    content.fullscreen(picture)
                }
            ) {
                Image(
                    Image.makeFromEncoded(
                        toByteArray(picture.image)
                    ).asImageAsset(),
                    modifier = Modifier.preferredHeight(70.dp)
                        .preferredWidth(90.dp)
                        .padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 1.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = picture.name,
                color = Foreground,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.body1
            )
                
            Clickable(
                modifier = Modifier.preferredHeight(70.dp)
                    .preferredWidth(30.dp)
                    .hover(
                        onEnter = {
                            infoButtonHover.value = true
                            false
                        },
                        onExit = {
                            infoButtonHover.value = false
                            false
                    })
                    .background(color = if (infoButtonHover.value) TranslucentWhite else Transparent),
                onClick = {
                    showPopUpMessage(
                        "${ResString.picture} " +
                        "${picture.name} \n" +
                        "${ResString.size} " +
                        "${picture.width}x${picture.height} " +
                        "${ResString.pixels}"
                    )
                }
            ) {
                Image(
                    icDots(),
                    modifier = Modifier.preferredHeight(70.dp)
                        .preferredWidth(30.dp)
                        .padding(start = 1.dp, top = 25.dp, end = 1.dp, bottom = 25.dp),
                    contentScale = ContentScale.FillHeight
                )
            }
        }
    }
}

@Composable
fun setScrollableArea(content: ContentState) {
    Box(
        modifier = Modifier.fillMaxSize()
        .padding(end = 8.dp)
    ) {
        val stateVertical = rememberScrollState(0f)
        ScrollableColumn(scrollState = stateVertical) {
            var index = 1
            Column {
                for (picture in content.getMiniatures()) {
                    setMiniatureUI(
                        picture = picture,
                        content = content
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    index++
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(stateVertical),
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}

@Composable
fun setDivider() {

    Divider(
        color = LightGray,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
}

@Composable
fun setSpacer(h: Int) {

    Spacer(modifier = Modifier.height(h.dp))
}
