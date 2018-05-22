package com.google.r4a.examples.explorerapp.infinitescroll

import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import android.support.v7.widget.*
import android.view.Gravity
import android.view.ViewGroup
import com.google.r4a.adapters.LocalUtils.stringToIntPx
import com.google.r4a.components.Recycler
import com.google.r4a.examples.explorerapp.R

class NewsFeed: Component() {
    private val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    // TODO(lmr): do a better job dealing with layoutmanager...
    private val layoutManager = LinearLayoutManager(null).apply { setOrientation(LinearLayoutManager.VERTICAL) }

    override fun compose() {
        <Recycler
            layoutParams={layoutParams}
            layoutManager={layoutManager}
            getItemCount={object: Function0<Int> {
                override fun invoke(): Int {
                    return 9999
                }
            }}
            composeItem={object: Function1<Int, Unit> {
                override fun invoke(position: Int) {
                    <ItemComponent position={position} />
                }
            }}

        />
    }
}


class ItemComponent: Component() {
    var position = 0
    override fun compose() {

        val relLayout = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val imgLayout = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, stringToIntPx("180dp"))

        val tvLayout = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            addRule(RelativeLayout.BELOW, R.id.img)
            leftMargin = stringToIntPx("5dp")
            setMarginStart(stringToIntPx("5dp"))
        }

        val imgButtonLayout = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            addRule(RelativeLayout.BELOW, R.id.img)
            rightMargin = stringToIntPx("5dp")
            setMarginEnd(stringToIntPx("5dp"))
        }

        <CardView
            cardBackgroundColor={Color.WHITE}
            radius="0dp"
            maxCardElevation="1dp"
            cardElevation="0.7dp"
            contentPadding="10dp"
            contentPaddingBottom="0dp"
            preventCornerOverlap={true}
            useCompatPadding={true}
        >
            <RelativeLayout layoutParams={relLayout}>
                <ImageView
                    id={R.id.img}
                    layoutParams={imgLayout}
                    scaleType="center_crop"
                    uri="https://picsum.photos/200/300?image=$position"
                    contentDescription="CardImageViewDesc"
                />
                <TextView
                    layoutParams={tvLayout}
                    fontFamily="sans-serif"
                    gravity={Gravity.CENTER_VERTICAL}
                    text="Joshua Sortino - Via Unsplash"
                    textSize="18sp"
                />
                <ImageButton
                    layoutParams={imgButtonLayout}
//                    backgroundResource={android.support.v7.appcompat.R.attr.selectableItemBackgroundBorderless}
//                    imageResource={android.support.v7.appcompat.R.drawable.abc_ic_voice_search_api_material}
                    contentDescription="FavButtonDesc"
                />
            </RelativeLayout>
        </CardView>
    }
}