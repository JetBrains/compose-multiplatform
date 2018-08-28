package com.google.r4a.examples.explorerapp.ui.infinitescroll


import android.view.ViewGroup
import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT


/** This component represents an individual feed story, including a title, optional description, and photo **/
class NewsFeedStoryComponent : Component() {

    var story: Story? = null

    override fun compose() {
        val story = story
        <LinearLayout
            orientation=LinearLayout.VERTICAL
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
            marginTop=125.px
            marginBottom=250.px
            minimumHeight=1000
        >
            if (story == null) {
                // If we don't have a newsfeed story yet, show a loading spinner instead
                <ProgressBar />
            } else {
                <TextView
                    text=story.name
                    textSize=30f
                    paddingLeft=25
                    paddingBottom=10
                />
                if (story.description != null && story.description.isNotEmpty()) {
                    <TextView
                        text=story.description
                        textSize=20f
                        paddingLeft=25
                        paddingBottom=10
                    />
                }
                <ImageView
                    imageBitmap=story.image
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    scaleType=ImageView.ScaleType.CENTER_CROP
                />
            }
        </LinearLayout>
    }
}
