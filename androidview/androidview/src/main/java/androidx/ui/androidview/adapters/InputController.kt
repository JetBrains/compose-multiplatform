package androidx.ui.androidview.adapters

import android.view.View
import android.view.ViewTreeObserver

// This class is a small helper class for creating objects that properly deal with controlled
// inputs. The expectation is that you will implement this class and have the implementation also
// be a listener object for whatever "change" event you are wanting to listen to. This class assumes
// that there is a "value" attribute and corresponding "onChange" attribute that you are passing
// directly into this class. This class will call the view's setter only when necessary, but will
// also ensure that the consumer of the view is properly calling recompose() and setting the view
// after an event gets fired, or else it will correctly "un-change" the view to whatever it was last
// set to.
abstract class InputController<V : View, T>(
    protected val view: V
) : ViewTreeObserver.OnPreDrawListener {
    @Suppress("LeakingThis")
    private var lastSetValue: T = getValue()

    // TODO(malkov): subject to change when binding in adapters will be introduced
    // TODO(lmr): this doesn't work anymore and the APIs it's using are deprecated, so commenting
    //  out for now until we fix.
    /*private fun inCompositionContext(action: CompositionContext.(Component) -> Unit)) {
        CompositionContext.findRoot(view)?.let { root ->
            CompositionContext.find(root)?.action(root)
        }
    }*/

    protected abstract fun getValue(): T
    protected abstract fun setValue(value: T)
    protected fun prepareForChange(@Suppress("UNUSED_PARAMETER") value: T) {
        /*inCompositionContext {
            addPostRecomposeObserver(onPostRecompose)
        }*/
        // TODO(malkov): remove it then we can control lifecycle of InputController
        // for now we don't have proper ways to dispose this listener when view goes out of
        // recompose scope, so we have to add and remove listener every time
        view.viewTreeObserver.addOnPreDrawListener(this)
    }

    fun setValueIfNeeded(value: T) {
        val current = getValue()
        lastSetValue = value
        if (current != value) {
            setValue(value)
        }
    }

    val onPostRecompose: () -> Unit = {
        if (lastSetValue != getValue()) setValueIfNeeded(lastSetValue)
    }

    override fun onPreDraw(): Boolean {
        /*inCompositionContext {
            removePostRecomposeObserver(onPostRecompose)
        }*/
        // TODO(malkov): remove it then we can control lifecycle of InputController
        // for now we don't have proper ways to dispose this listener when view goes out of
        // recompose scope, so we have to add and remove listener every time
        view.viewTreeObserver.removeOnPreDrawListener(this)

        if (lastSetValue == getValue()) return true
        /*inCompositionContext { root ->
            // TODO(lmr): figure out right way to do this
              recomposeSync(root)
        }*/

        if (lastSetValue == getValue()) return true
        setValueIfNeeded(lastSetValue)

        return true
    }
}