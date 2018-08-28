package com.google.r4a.examples.explorerapp.ui.infinitescroll

import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.widget.*
import android.support.v7.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import android.view.Gravity
import android.view.ViewGroup
import com.google.r4a.examples.explorerapp.ui.R
import com.google.r4a.examples.explorerapp.ui.components.Recycler
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class NewsFeed: Component() {
    // TODO(lmr): do a better job dealing with layoutmanager...
    private val layoutManager = LinearLayoutManager(null).apply { setOrientation(LinearLayoutManager.VERTICAL) }

    override fun compose() {
        <Recycler
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
            layoutManager
            getItemCount={ 9999 }
        > position ->
            <ItemComponent position />
        </Recycler>
    }
}


class ItemComponent: Component() {
    var position = 0
    override fun compose() {

        <CardView
            cardBackgroundColor=Color.WHITE
            radius=0.dp
            maxCardElevation=1.dp
            cardElevation=0.7.dp
            contentPaddingHorizontal=10.dp
            contentPaddingTop=10.dp
            contentPaddingBottom=0.dp
            preventCornerOverlap=true
            useCompatPadding=true
        >
            <RelativeLayout
                layoutWidth=MATCH_PARENT
                layoutHeight=MATCH_PARENT
            >
                <ImageView
                    id=R.id.img
                    layoutWidth=MATCH_PARENT
                    layoutHeight=180.dp
                    scaleType="center_crop"
                    uri="https://picsum.photos/200/300?image=$position"
                    contentDescription="CardImageViewDesc"
                />
                <TextView
                    layoutWidth=MATCH_PARENT
                    layoutHeight=MATCH_PARENT
                    layoutBelow=R.id.image
                    marginLeft=5.dp
                    fontFamily="sans-serif"
                    gravity=Gravity.CENTER_VERTICAL
                    text="Joshua Sortino - Via Unsplash"
                    textSize=18.sp
                />
                <ImageButton
                    layoutWidth=WRAP_CONTENT
                    layoutHeight=MATCH_PARENT
                    layoutAlignParentBottom=true
                    layoutAlignParentRight=true
                    layoutBelow=R.id.img
//                    backgroundResource={android.support.v7.appcompat.R.attr.selectableItemBackgroundBorderless}
//                    imageResource={android.support.v7.appcompat.R.drawable.abc_ic_voice_search_api_material}
                    contentDescription="FavButtonDesc"
                />
            </RelativeLayout>
        </CardView>
    }
}