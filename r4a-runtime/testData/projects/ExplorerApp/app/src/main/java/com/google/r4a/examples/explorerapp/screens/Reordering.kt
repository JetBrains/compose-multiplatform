package com.google.r4a.examples.explorerapp.screens

import com.google.r4a.Component
import android.widget.*
import android.view.*
import com.google.r4a.CompositionContext
import com.google.r4a.AttributeAdapterLocal
import com.google.r4a.CompositionContextImpl

class Reordering : Component() {
    // state
    private var items = mutableListOf(1, 2, 3, 4, 5)

    private fun onMove(index: Int): Function1<Int, Unit> {
        return object: Function1<Int, Unit> {
            override fun invoke(amount: Int) {
                val next = index + amount
                if (next < 0 || next >= items.size) return
                val item = items.removeAt(index)
                // TODO: immutable list ops would be better
                items.add(next, item)
                recompose()
            }
        }
    }

    private val printTreeHandler = object: View.OnClickListener {
        override fun onClick(v: View?) {
            val cc = CompositionContext.find(this@Reordering) as CompositionContextImpl
            cc.printSlots()
        }
    }

    override fun compose() {
        <LinearLayout orientation={LinearLayout.VERTICAL}>
            <Button
                text="PRINT TREE"
                onClickListener={printTreeHandler}
            />
            items.forEachIndexed { index, id ->
                <Item key={id} id={id} onMove={onMove(index)} />
            }
        </LinearLayout>
    }

    private class Item: Component() {
        // props
        var id: Int = 0
        lateinit var onMove: (Int) -> Unit

        // state
        private var count: Int = 0

        private val onIncrement = object: View.OnClickListener {
            override fun onClick(v: View?) {
                count += 1
                recompose()
            }
        }

        private fun onMoveMake(amount: Int): View.OnClickListener {
            return object: View.OnClickListener {
                override fun onClick(v: View?) {
                    onMove(amount)
                }
            }
        }

        override fun compose() {
            <LinearLayout orientation={LinearLayout.HORIZONTAL}>
                <TextView text="id: $id amt: $count" textSize={20f} />
                <Button text="+" onClickListener={onIncrement} />
                <Button text="Up" onClickListener={onMoveMake(1)} />
                <Button text="Down" onClickListener={onMoveMake(-1)} />
            </LinearLayout>
        }
    }
}

