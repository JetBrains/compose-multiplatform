package example.imageviewer.view

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Surface
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageAsset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.common.R
import example.imageviewer.model.AppState
import example.imageviewer.model.Picture
import example.imageviewer.model.ScreenType
import example.imageviewer.model.ContentState
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.Transparent
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.LightGray
import example.imageviewer.style.icRefresh
import example.imageviewer.style.icEmpty
import example.imageviewer.style.icDots


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
fun setLoadingScreen(content: ContentState) {

    Box {
        Column {
            setTopContent(content)
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            Surface(color = DarkGray, elevation = 4.dp, shape = CircleShape) {
                CircularProgressIndicator(
                    modifier = Modifier.preferredSize(50.dp).padding(4.dp),
                    color = DarkGreen
                )
            }
        }
        Text(
            text = content.getString(R.string.loading),
            modifier = Modifier.align(Alignment.Center).offset(0.dp, 70.dp),
            style = MaterialTheme.typography.body1,
            color = Foreground
        )
    }
}

@Composable
fun setTopContent(content: ContentState) {

    setTitleBar(text = "ImageViewer", content = content)
    if (content.getOrientation() == Configuration.ORIENTATION_PORTRAIT) {
        setPreviewImageUI(content)
        setSpacer(h = 10)
        setDivider()
    }
    setSpacer(h = 5)
}

@Composable
fun setTitleBar(text: String, content: ContentState) {

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

    Clickable(onClick = {
        AppState.screenState(ScreenType.FullscreenImage)
    }) {
        Card(
            backgroundColor = DarkGray,
            modifier = Modifier.preferredHeight(250.dp),
            shape = RectangleShape,
            elevation = 1.dp
        ) {
            Image(
                if (content.isMainImageEmpty()) {
                    icEmpty()
                }
                else {
                    content.getSelectedImage().asImageAsset()
                },
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

    Card(
        backgroundColor = MiniatureColor,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp).preferredHeight(70.dp)
            .fillMaxWidth()
            .clickable {
                content.setMainImage(picture)
            },
        shape = RectangleShape,
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Clickable(
                onClick = {
                    content.fullscreen(picture)
                }
            ) {
                Image(
                    picture.image.asImageAsset(),
                    modifier = Modifier.preferredHeight(70.dp)
                        .preferredWidth(90.dp)
                        .padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 1.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = picture.name,
                color = Foreground,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically).padding(start = 16.dp),
                style = MaterialTheme.typography.body1
            )

            Clickable(
                modifier = Modifier.preferredHeight(70.dp)
                        .preferredWidth(30.dp),
                onClick = {
                    showPopUpMessage(
                        "${content.getString(R.string.picture)} " +
                        "${picture.name} \n" +
                        "${content.getString(R.string.size)} " +
                        "${picture.width}x${picture.height} " +
                        "${content.getString(R.string.pixels)}",
                        content.getContext()
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

    ScrollableColumn {
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
