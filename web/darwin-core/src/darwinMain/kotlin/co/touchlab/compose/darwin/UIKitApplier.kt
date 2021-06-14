package co.touchlab.compose.darwin

import androidx.compose.runtime.AbstractApplier
import kotlinx.cinterop.convert
import platform.UIKit.UIView
import platform.UIKit.insertSubview
import platform.UIKit.removeFromSuperview
import platform.UIKit.subviews
import platform.darwin.NSInteger

class UIKitApplier(root: UIViewWrapper) : AbstractApplier<UIViewWrapper>(root) {
  override fun onClear() {
    // or current.node.clear()?; in all examples it calls 'clear' on the root
    root.view.subviews.forEach { v ->
      (v as UIView).removeFromSuperview()
    }
  }

  override fun insertBottomUp(index: Int, instance: UIViewWrapper) {
    current.insert(index, instance)
  }

  override fun insertTopDown(index: Int, instance: UIViewWrapper) {
    // ignored. Building tree bottom-up
  }

  override fun move(from: Int, to: Int, count: Int) {
    current.move(from, to, count)
  }

  override fun remove(index: Int, count: Int) {
    current.remove(index, count)
  }
}

class UIViewWrapper(val view: UIView){
  fun insert(index: Int, nodeWrapper: UIViewWrapper) {
    view.insertSubview(nodeWrapper.view, index.convert<NSInteger>())
  }

  fun remove(index: Int, count: Int) {
    view.subviews.subList(index, index+count).forEach { (it as UIView).removeFromSuperview() }
  }

  fun move(from: Int, to: Int, count: Int) {
    if (from == to) {
      return // nothing to do
    }

    val allViews = view.subviews.subList(from, from + count).map { it as UIView }

    repeat(count) { vindex ->
      allViews[vindex].removeFromSuperview()
    }

    repeat(count) { vindex ->
      view.insertSubview(allViews[vindex], (to + vindex).convert<NSInteger>())
    }
  }
}