package com.google.r4a.examples.explorerapp.ui.screens

import android.graphics.Color
import android.graphics.Typeface
import android.support.v7.widget.CardView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.Component
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.components.CardImageView
import com.google.r4a.examples.explorerapp.common.components.ImageSpec
import com.google.r4a.examples.explorerapp.common.data.Link
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.components.TimeAgo
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class LinkCard(var link: Link) : Component() {
    var onClick: () -> Unit = {}

    override fun compose() {
        <CardView
            layoutWidth=MATCH_PARENT
            layoutHeight=WRAP_CONTENT
            marginLeft=16.dp
            marginTop=8.dp
            marginRight=16.dp
            cardBackgroundColor=Color.WHITE
            radius=3.dp
            maxCardElevation=1.dp
            cardElevation=0.7.dp
            preventCornerOverlap=true
            foreground=android.R.attr.selectableItemBackground
            clickable=true
            onClick=onClick
            useCompatPadding=true
        >
            <LinearLayout
                orientation=LinearLayout.VERTICAL
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
            >
                val image = link.preview?.images?.firstOrNull()
                if (image != null) {
                    // image will at worst be square
                    <CardImageView
                        spec=ImageSpec(
                                uri = image.source.url,
                                aspectRatio = Math.min(1f, image.source.height.toFloat() / image.source.width.toFloat())
                        )
                        layoutWidth=MATCH_PARENT
                        layoutHeight=WRAP_CONTENT
                    />
                }
                <LinearLayout
                    orientation=LinearLayout.VERTICAL
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    paddingHorizontal=10.dp
                    paddingVertical=10.dp
                >
                    <TextView
                        text=link.title
                        textColor=Colors.TEXT_DARK
                        textSize=8.sp
                    />

                    // TODO: number of comments, subreddit, domain

                    <LinearLayout
                        orientation=LinearLayout.HORIZONTAL
                        layoutWidth=MATCH_PARENT
                        layoutHeight=WRAP_CONTENT
                    >
                        <TextView
                            text=link.author
                            fontStyle=Typeface.BOLD
                            textColor=Colors.TEXT_MUTED
                        />
                        if (link.score != 0) {
                            <Bullet />
                            <TextView
                                text="${link.score}"
                                textColor=Colors.TEXT_MUTED
                            />
                        }
                        <Bullet />
                        <TimeAgo
                            date=link.createdUtc
                            textColor=Colors.TEXT_MUTED
                        />
                    </LinearLayout>
                </LinearLayout>
            </LinearLayout>
        </CardView>
    }
}