package com.google.r4a.examples.explorerapp.ui.screens

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.CompositionContext
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import com.google.r4a.examples.explorerapp.ui.components.TimeAgo
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class CommentRow(
    var node: HierarchicalThing,
    var onClick: () -> Unit
) : Component() {
    override fun compose() {
        val node = node
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
}

class HorizontalDivider : Component() {
    override fun compose() {
        <View
            layoutWidth=MATCH_PARENT
            layoutHeight=1.dp
            backgroundColor=Colors.LIGHT_GRAY
        />
    }
}

class Bullet : Component() {
    override fun compose() {
        <TextView text=" · " textColor=Colors.TEXT_MUTED />
    }
}

class CommentAuthorLine(var author: String) : Component() {
    var score: Int = 0
    var createdUtc: Long = 0
    var collapseCount: Int = 0
    override fun compose() {
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
                    textSize=5.sp
                    text="$collapseCount"
                />
            }
        </LinearLayout>
    }
}