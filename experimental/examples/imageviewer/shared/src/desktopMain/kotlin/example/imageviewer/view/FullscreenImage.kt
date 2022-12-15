//package example.imageviewer.view
//
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.interaction.collectIsHoveredAsState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.MaterialTheme
//import androidx.compose.material.Surface
//import androidx.compose.material.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.ExperimentalComposeUiApi
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.painter.Painter
//import androidx.compose.ui.input.key.Key
//import androidx.compose.ui.input.key.key
//import androidx.compose.ui.input.key.type
//import androidx.compose.ui.input.key.KeyEventType
//import androidx.compose.ui.input.key.onPreviewKeyEvent
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.unit.dp
//import example.imageviewer.core.FilterType
//import example.imageviewer.model.AppState
//import example.imageviewer.model.ContentState
//import example.imageviewer.model.ScreenType
//import example.imageviewer.ResString
//import example.imageviewer.style.DarkGray
//import example.imageviewer.style.Foreground
//import example.imageviewer.style.MiniatureColor
//import example.imageviewer.style.TranslucentBlack
//import example.imageviewer.style.Transparent
//
//@Composable
//fun ToolBar(
//    text: String,
//    content: ContentState
//) {
//    val backButtonInteractionSource = remember { MutableInteractionSource() }
//    val backButtonHover by backButtonInteractionSource.collectIsHoveredAsState()
//    Surface(
//        color = MiniatureColor,
//        modifier = Modifier.height(44.dp)
//    ) {
//        Row(modifier = Modifier.padding(end = 30.dp)) {
//            Surface(
//                color = Transparent,
//                modifier = Modifier.padding(start = 20.dp).align(Alignment.CenterVertically),
//                shape = CircleShape
//            ) {
//                Tooltip(ResString.back) {//todo Tooltip
//                    Clickable(
//                        modifier = Modifier
//                            .hoverable(backButtonInteractionSource)
//                            .background(color = if (backButtonHover) TranslucentBlack else Transparent),
//                        onClick = {
//                            if (content.isContentReady()) {
//                                content.restoreMainImage()
//                                AppState.screenState(ScreenType.MainScreen)
//                            }
//                        }) {
//                        Image(
//                            icBack(),
//                            contentDescription = null,
//                            modifier = Modifier.size(38.dp)
//                        )
//                    }
//                }
//            }
//            Text(
//                text,
//                color = Foreground,
//                maxLines = 1,
//                modifier = Modifier.padding(start = 30.dp).weight(1f)
//                    .align(Alignment.CenterVertically),
//                style = MaterialTheme.typography.body1
//            )
//
//            Surface(
//                color = Color(255, 255, 255, 40),
//                modifier = Modifier.size(154.dp, 38.dp)
//                    .align(Alignment.CenterVertically),
//                shape = CircleShape
//            ) {
//                val state = rememberScrollState(0)
//                Row(modifier = Modifier.horizontalScroll(state)) {
//                    Row {
//                        for (type in FilterType.values()) {
//                            FilterButton(content, type)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun Image(content: ContentState) {//todo
//    val onUpdate = remember { { content.updateMainImage() } }
//    Surface(
//        color = DarkGray,
//        modifier = Modifier.fillMaxSize()
//    ) {
//        Draggable(
//            onUpdate = onUpdate,
//            dragHandler = content.drag,
//            modifier = Modifier.fillMaxSize()
//        ) {
//            Zoomable(
//                onUpdate = onUpdate,
//                scaleHandler = content.scale,
//                modifier = Modifier.fillMaxSize()
//                    .onPreviewKeyEvent {
//                        if (it.type == KeyEventType.KeyUp) {
//                            when (it.key) {
//                                Key.DirectionLeft -> {
//                                    content.swipePrevious()
//                                }
//                                Key.DirectionRight -> {
//                                    content.swipeNext()
//                                }
//                            }
//                        }
//                        false
//                    }
//            ) {
//                Image(
//                    bitmap = content.getSelectedImage(),
//                    contentDescription = null,
//                    contentScale = ContentScale.Fit
//                )
//            }
//        }
//    }
//}
