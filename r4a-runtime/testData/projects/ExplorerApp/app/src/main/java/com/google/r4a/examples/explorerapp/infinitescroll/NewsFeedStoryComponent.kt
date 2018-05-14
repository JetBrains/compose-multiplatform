package com.google.r4a.examples.explorerapp.infinitescroll


import android.widget.*
import com.google.r4a.Component
import com.google.r4a.AttributeAdapterLocal

/** This component represents an individual feed story, including a title, optional description, and photo **/
class NewsFeedStoryComponent : Component() {

    var story: Story? = null

    private val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT).apply {
        topMargin = 125
        bottomMargin = 250
    }

    override fun compose() {
        val story = story
        <LinearLayout
            orientation="vertical"
            layoutParams={layoutParams}
            minimumHeight={1000}
        >
            if (story == null) {
                // If we don't have a newsfeed story yet, show a loading spinner instead
                <ProgressBar />
            } else {
                <TextView
                    text={story.name}
                    textSize={30f}
                    paddingLeft={25}
                    paddingBottom={10}
                />
                if (story.description != null && story.description.isNotEmpty()) {
                    <TextView
                        text={story.description}
                        textSize={20f}
                        paddingLeft={25}
                        paddingBottom={10}
                    />
                }
                <ImageView
                    imageBitmap={story.image}
                    layoutParams={LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)}
                    scaleType={ImageView.ScaleType.CENTER_CROP}
                />
            }
        </LinearLayout>
    }
}
