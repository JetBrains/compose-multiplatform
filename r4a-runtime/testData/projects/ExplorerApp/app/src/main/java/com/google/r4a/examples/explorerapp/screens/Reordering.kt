package com.google.r4a.examples.explorerapp.screens

import com.google.r4a.*
import com.google.r4a.adapters.*
import android.widget.*
import android.view.*
import com.google.r4a.CompositionContext

class Reordering : Component() {
    // state
    private var items = mutableListOf(1, 2, 3, 4, 5)

    private fun onMove(index: Int): Function1<Int, Unit> {
        return { amount ->
            val next = index + amount
            if (next >= 0 && next < items.size) {
                val item = items.removeAt(index)
                // TODO: immutable list ops would be better
                items.add(next, item)
                recompose()
            }
        }
    }

    private val printTreeHandler = object: View.OnClickListener {
        override fun onClick(v: View?) {
            val cc = CompositionContext.find(this@Reordering)
            if (cc != null) {
                cc.debug()
            }
        }
    }

    override fun compose() {
        <LinearLayout orientation=LinearLayout.VERTICAL>
            <Button
                text="PRINT TREE"
                onClickListener=printTreeHandler
            />
            items.forEachIndexed { index, id ->
                <Item key=id id onMove=onMove(index) />
            }
        </LinearLayout>
    }

    private class Item: Component() {
        // props
        var id: Int = 0
        lateinit var onMove: (Int) -> Unit

        // state
        private var count: Int = 0

        override fun compose() {
            <LinearLayout orientation=LinearLayout.HORIZONTAL>
                <TextView text="id: $id amt: $count" textSize=20f />
                <Button text="+" onClick={ count++; recompose() } />
                <Button text="Up" onClick={ onMove(1) } />
                <Button text="Down" onClick={ onMove(-1) } />
            </LinearLayout>
        }
    }
}

