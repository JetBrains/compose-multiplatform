package com.google.r4a.examples.explorerapp.infinitescroll


import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.google.r4a.CompositionContext


class PhotoManagementRecyclerAdapter : RecyclerView.Adapter<NewsFeedItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NewsFeedItemViewHolder(parent.getContext())

    override fun onBindViewHolder(holder: NewsFeedItemViewHolder, position: Int) {
        holder.component.story = null // Reset the view, so we're not showing old data while the new story loads asynchronously
        val cc = CompositionContext.find(holder.component)
        if (cc == null) {
            println("couldnt find composition context")
            return
        }
        val callback = object: Function1<Story, Unit> {
            override fun invoke(story: Story) {
                holder.component.story = story // Reset the view, so we're not showing old data while the new story loads asynchronously
                cc.recompose(holder.component)
            }
        }
        MyBackendDataApi.getStory(holder.itemView.getContext(), position, callback)
    }

    override fun getItemCount() = 9999
}
