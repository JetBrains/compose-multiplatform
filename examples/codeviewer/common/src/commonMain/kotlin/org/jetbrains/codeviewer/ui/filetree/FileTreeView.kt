package org.jetbrains.codeviewer.ui.filetree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AmbientContentColor
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.codeviewer.platform.VerticalScrollbar
import org.jetbrains.codeviewer.platform.pointerMoveFilter
import org.jetbrains.codeviewer.util.withoutWidthConstraints

@Composable
fun FileTreeViewTabView() = Surface {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Files",
            color = AmbientContentColor.current.copy(alpha = 0.60f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun FileTreeView(model: FileTree) = Surface(
    modifier = Modifier.fillMaxSize()
) {
    with(DensityAmbient.current) {
        Box {
            val scrollState = rememberLazyListState()
            val fontSize = 14.sp
            val lineHeight = fontSize.toDp() * 1.5f

            LazyColumnFor(
                model.items,
                modifier = Modifier.fillMaxSize().withoutWidthConstraints(),
                state = scrollState,
                itemContent = { FileTreeItemView(fontSize, lineHeight, it) }
            )

            VerticalScrollbar(
                Modifier.align(Alignment.CenterEnd),
                scrollState,
                model.items.size,
                lineHeight
            )
        }
    }
}

@Composable
private fun FileTreeItemView(fontSize: TextUnit, height: Dp, model: FileTree.Item) = Row(
    modifier = Modifier
        .wrapContentHeight()
        .clickable { model.open() }
        .padding(start = 24.dp * model.level)
        .height(height)
        .fillMaxWidth()
) {
    val active = remember { mutableStateOf(false) }

    FileItemIcon(Modifier.align(Alignment.CenterVertically), model)
    Text(
        text = model.name,
        color = if (active.value) AmbientContentColor.current.copy(alpha = 0.60f) else AmbientContentColor.current,
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .clipToBounds()
            .pointerMoveFilter(
                onEnter = {
                    active.value = true
                    true
                },
                onExit = {
                    active.value = false
                    true
                }
            ),
        softWrap = true,
        fontSize = fontSize,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@Composable
private fun FileItemIcon(modifier: Modifier, model: FileTree.Item) = Box(modifier.size(24.dp).padding(4.dp)) {
    when (val type = model.type) {
        is FileTree.ItemType.Folder -> when {
            !type.canExpand -> Unit
            type.isExpanded -> Icon(Icons.Default.KeyboardArrowDown, tint = AmbientContentColor.current)
            else -> Icon(Icons.Default.KeyboardArrowRight, tint = AmbientContentColor.current)
        }
        is FileTree.ItemType.File -> when (type.ext) {
            "kt" -> Icon(Icons.Default.Code, tint = Color(0xFF3E86A0))
            "xml" -> Icon(Icons.Default.Code, tint = Color(0xFFC19C5F))
            "txt" -> Icon(Icons.Default.Description, tint = Color(0xFF87939A))
            "md" -> Icon(Icons.Default.Description, tint = Color(0xFF87939A))
            "gitignore" -> Icon(Icons.Default.BrokenImage, tint = Color(0xFF87939A))
            "gradle" -> Icon(Icons.Default.Build, tint = Color(0xFF87939A))
            "kts" -> Icon(Icons.Default.Build, tint = Color(0xFF3E86A0))
            "properties" -> Icon(Icons.Default.Settings, tint = Color(0xFF62B543))
            "bat" -> Icon(Icons.Default.Launch, tint = Color(0xFF87939A))
            else -> Icon(Icons.Default.TextSnippet, tint = Color(0xFF87939A))
        }
    }
}