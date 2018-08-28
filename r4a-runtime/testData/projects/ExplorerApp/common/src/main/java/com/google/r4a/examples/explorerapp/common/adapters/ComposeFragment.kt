package com.google.r4a.examples.explorerapp.common.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.findNavController
import com.google.r4a.*

/**
 * A fragment that uses a Component as it's UI.
 */
abstract class ComposeFragment : Fragment() {
    var reference: Ambient.Reference? = null
    abstract fun compose()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // NOTE(lmr): Should we use FrameLayout or LinearLayout by default?
        val root = LinearLayout(inflater.context).apply {
            orientation = LinearLayout.VERTICAL
        }
        // TODO(lmr): right now we use container.findAmbientReference() as a way to allow ComposeFragment work
        // seamlessly with FragmentComponent. It works but feels a little bit wrong. Consider better options.
        val reference = reference ?: container?.findAmbientReference()
        R4a.composeInto(root, reference) {
            with(CompositionContext.current) {
                provideAmbient(Ambients.Fragment, this@ComposeFragment) {
                    group(0) {
                        compose()
                    }
                }
            }
        }
        return root
    }
}
