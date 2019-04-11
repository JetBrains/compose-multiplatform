package com.google.r4a.examples.explorerapp.ui.screens

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.Composable
import androidx.ui.androidview.adapters.*
import com.google.r4a.composer
import com.google.r4a.examples.explorerapp.common.adapters.setForeground
import com.google.r4a.examples.explorerapp.common.data.Comment
import com.google.r4a.examples.explorerapp.common.data.HierarchicalThing
import com.google.r4a.examples.explorerapp.common.data.RedditMore
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.components.TimeAgo



@Composable
fun CommentRow(
    node: HierarchicalThing,
    onClick: () -> Unit
) {
    val depth = node.depth


    <LinearLayout
        orientation=LinearLayout.HORIZONTAL
        layoutWidth=MATCH_PARENT
        layoutHeight=MATCH_PARENT
        paddingLeft=(depth * 16.dp)
    >

        <LinearLayout
            orientation=LinearLayout.VERTICAL
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
            backgroundColor=Colors.WHITE
            foreground=android.R.attr.selectableItemBackground
            clickable=true
            onClick
        >
            // TODO(typeres): why doesn't this tag have a ref target?
            <HorizontalDivider />
            <LinearLayout
                orientation=LinearLayout.VERTICAL
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                paddingHorizontal=16.dp
                paddingVertical=8.dp
            >
                when (node) {
                    is Comment -> {
                        <CommentAuthorLine
                            author=node.author
                            collapseCount=(node.collapsedChildren?.size ?: 0)
                            score=node.score
                            createdUtc=node.createdUtc
                        />
                        <TextView text=node.body />
                    }
                    is RedditMore -> {
                        <TextView
                            fontStyle=Typeface.ITALIC
                            text="Load ${node.children.size} more comments"
                        />
                    }
                    else -> error("unrecognized node type!")
                }
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
}

@Composable
fun HorizontalDivider() {
    <View
        layoutWidth=MATCH_PARENT
        layoutHeight=1.dp
        backgroundColor=Colors.LIGHT_GRAY
    />
}

@Composable
fun Bullet() {
    <TextView text=" · " textColor=Colors.TEXT_MUTED />
}

@Composable
fun CommentAuthorLine(
    author: String,
    score: Int = 0,
    createdUtc: Long = 0,
    collapseCount: Int = 0
) {
    <LinearLayout
        orientation=LinearLayout.HORIZONTAL
        layoutWidth=MATCH_PARENT
        layoutHeight=WRAP_CONTENT
    >
        <TextView
            text=author
            fontStyle=Typeface.BOLD
            textColor=Colors.TEXT_MUTED
        />
        if (score != 0) {
            <Bullet />
            <TextView
                text="$score"
                textColor=Colors.TEXT_MUTED
            />
        }
        <Bullet />
        <TimeAgo
            date=createdUtc
            textColor=Colors.TEXT_MUTED
        />
        if (collapseCount != 0) {
            <TextView
                padding=10.dp
                textColor=Colors.WHITE
                backgroundColor=Colors.SECONDARY
                textSize=13.sp
                text="$collapseCount"
            />
        }
    </LinearLayout>
}