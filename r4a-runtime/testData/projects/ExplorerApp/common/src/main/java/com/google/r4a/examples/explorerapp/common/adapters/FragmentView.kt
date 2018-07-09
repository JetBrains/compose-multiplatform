package com.google.r4a.examples.explorerapp.common.adapters

import android.content.Context
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.R

class FragmentComponent : Component() {
    lateinit var construct: () -> Fragment
    var layoutParams: ViewGroup.LayoutParams? = null
    var id: Int = 0
    override fun compose() {
        with(CompositionContext.current) {
            // NOTE(lmr): if we use R4aContext as ambient reference we can probably get rid of this component
            // entirely
            portal(0) { ref ->
                emitView(0, { FragmentView(it, construct, id, ref) }) {
                    set(layoutParams) { layoutParams = it }
                    set(id) { id = it }
                }
            }
        }
    }
}

private class FragmentView(
        context: Context,
        construct: () -> Fragment,
        thisId: Int,
        reference: Ambient.Reference?
) : FrameLayout(context) {
    private var fragmentManager = CompositionContext.current.getAmbient(Ambients.FragmentManager)
    init {
        val transaction = fragmentManager.beginTransaction()
        val f = construct()
        // TODO(lmr): Right now we need a way to pass ambients through to the fragment that we create for
        // cases where the fragment is a ComposeFragment. In this case we store the ambient reference onto the view
        // and then have ComposeFragment walk up the tree to try and find it. This is obviously not optimal, and I think
        // if we go down the route of having an R4aContext or something and store the ambient reference then it won't
        // be an issue?
        if (reference != null) {
            setAmbientReference(reference)
        }
        transaction.add(thisId, f)
        transaction.commitNow()
    }
}


internal fun View.setAmbientReference(ref: Ambient.Reference) {
    setTag(R.id.ambient_reference_tag_key, ref)
}

internal fun View.findAmbientReference(): Ambient.Reference? {
    var node: View? = this

    while (node != null) {
        val value = node.getTag(R.id.ambient_reference_tag_key) as? Ambient.Reference
        if (value != null) return value
        node = node.parent as? View
    }
    return null
}

