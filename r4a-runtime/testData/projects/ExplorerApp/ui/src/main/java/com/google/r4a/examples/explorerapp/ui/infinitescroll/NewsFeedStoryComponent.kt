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
            marginTop=125.dp
            marginBottom=250.dp
            minimumHeight=1000.dp
        >
            if (story == null) {
                // If we don't have a newsfeed story yet, show a loading spinner instead
                <ProgressBar />
            } else {
                <TextView
                    text=story.name
                    textSize=30.sp
                    paddingLeft=25.dp
                    paddingBottom=10.dp
                />
                if (story.description != null && story.description.isNotEmpty()) {
                    <TextView
                        text=story.description
                        textSize=20.sp
                        paddingLeft=25.dp
                        paddingBottom=10.dp
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
