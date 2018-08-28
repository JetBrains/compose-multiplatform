package com.google.r4a.examples.explorerapp.common.adapters

import android.content.Context
import android.graphics.Color
import android.provider.CalendarContract
import android.support.design.widget.AppBarLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.r4a.Ambient
import com.google.r4a.Component
import com.google.r4a.CompositionContext
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.R


/**
 * A PagerAdapter subclass that constructs its pages using R4A.
 */
abstract class ComposePagerAdapter : PagerAdapter() {

    private val instantiated = SparseArray<FrameLayout>()

    var reference: Ambient.Reference? = null
    abstract fun composeItem(position: Int)

    fun recomposeAll() {
        val size = instantiated.size()
        for (i in 0 until size) {
            val index = instantiated.keyAt(i)
            val layout = instantiated.valueAt(i)
            compose(layout, index)
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
//        val key = Any()
//        root.setTag(R.id.key_compose_pager_adapter, key)

//        R4a.composeInto(container, reference) {
//            with(CompositionContext.current) {
//                emitFn(::composeItem, position)
//            }
//        }
//        return container


        val root = FrameLayout(container.context)
        container.addView(root)
        compose(root, position)
        instantiated.append(position, root)
//        instantiated.setValueAt(position, root)
        return root
    }

    private fun compose(container: ViewGroup, position: Int) {
        R4a.composeInto(container, reference) {
            with(CompositionContext.current) {
                group(0) {
                    composeItem(position)
                }
            }
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        instantiated.removeAt(position)
        // TODO(lmr): we don't yet have an "uncompose" top level API, but we should
        // R4a.unmountComponentAtView(container)
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === obj
    }

}

class ComposeViewPager : Component(), RefForwarder<ViewPager> {
    override val refToForward: Ref<ViewPager> = Ref()
    var children: (Int) -> Unit = {}
    var getCount: () -> Int = { 0 }
    var getPageTitle: (Int) -> CharSequence? = { null }
    var reference: Ambient.Reference? = null
    var offscreenPageLimit: Int = 1
    lateinit var layoutParams: ViewGroup.LayoutParams

    private val myAdapter = object : ComposePagerAdapter() {

        override fun composeItem(position: Int) {
            with(CompositionContext.current) {
                group(0) {
                    children(position)
                }
            }
        }

        override fun getCount(): Int = this@ComposeViewPager.getCount()
        override fun getPageTitle(position: Int) = this@ComposeViewPager.getPageTitle(position)
    }

    override fun compose() {
        // TODO(lmr): I think we realistically should call myAdapter.notifyDatasetChanged() here or in a
        // componentDidUpdate() like lifecycle
        with(CompositionContext.current) {
            portal(0) { ref ->
                myAdapter.reference = ref
                emitView(0, ::ViewPager) {
                    set(R.id.view_pager_id) { id = it }
                    set(myAdapter) { adapter = it }
                    set(refToForward) { setRef(it) }
                    set(offscreenPageLimit) { setOffscreenPageLimit(it) }
                }
            }
        }
        myAdapter.recomposeAll()
    }
}

/**
 * A Tabs component that abstracts away the common need of having a ViewPager and TabLayout work together.
 */
class Tabs : Component() {
    lateinit var titles: List<String>
    var offscreenPageLimit: Int = 1
    lateinit var tabLayoutParams: ViewGroup.LayoutParams
    var tabBackgroundColor: Int = 0
    lateinit var pagerLayoutParams: ViewGroup.LayoutParams
//    @Children lateinit var children: (
//            tabs: () -> Unit,
//            // NOTE(lmr): it would be nice to be able to annotate this with @Children
//            content: (children: (Int) -> Unit) -> Unit
//    ) -> Unit

    private var _children: (
        tabs: () -> Unit,
        content: (children: (Int) -> Unit) -> Unit
    ) -> Unit = { _, _ -> }

    @Children
    fun setChildren(children:@Composable() (
        tabs: () -> Unit,
        content: @Composable() (children: @Composable() (Int) -> Unit) -> Unit
    ) -> Unit) {
        _children = children
    }

    private val tabRef = Ref<TabLayout>()
    private val pagerRef = Ref<ViewPager>()

    private var didSetup = false

    fun componentDidMount() {
        // NOTE(lmr): This seems like a pretty valid usage of Lifecycles + Refs to me
        if (!didSetup) {
            didSetup = true
            val tl = tabRef.value
            val vp = pagerRef.value
            tl?.setupWithViewPager(vp)
        }
    }

    override fun compose() {
        with(CompositionContext.current) {
            group(0) {
                _children({
                    emitView(0, ::TabLayout) {
                        set(tabLayoutParams) { layoutParams = it }
//                    set(it, titles) { setTitles(it) }
                        set(tabBackgroundColor) { setBackgroundColor(it) }
                        set(tabRef) { setRef(it) }
                    }
                }, { composeTab ->
                    portal(0) { ambients ->
                        emitComponent(0, ::ComposeViewPager) {
                            set(pagerLayoutParams) { layoutParams = it }
                            set(ambients) { reference = it }
                            set(composeTab) { children = it }
                            set({ titles.size }) { getCount = it }
                            set({ position: Int -> titles[position] }) { getPageTitle = it }
                            set(pagerRef) { ref = it }
                            set(offscreenPageLimit) { offscreenPageLimit = it }
                        }
                    }
                })
            }
        }
        // TODO(lmr): eventually this will be a lifecycle. since we are sync on the main thread, it works
        // to just put it here
        componentDidMount()
    }
}