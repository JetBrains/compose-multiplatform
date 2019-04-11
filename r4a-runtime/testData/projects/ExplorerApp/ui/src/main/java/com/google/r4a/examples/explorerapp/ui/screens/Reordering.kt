package com.google.r4a.examples.explorerapp.ui.screens

import com.google.r4a.*
import androidx.ui.androidview.adapters.*
import android.widget.*

fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (to < from) return move(to, from)
    val item = get(from)
    val currentItem = get(to)
    val left = if (from > 0) subList(0, from) else emptyList()
    val right = if (to < size) subList(to + 1, size) else emptyList()
    val middle = if (to - from > 1) subList(from + 1, to) else emptyList()
    return left + listOf(currentItem) + middle + listOf(item) + right
}

@Composable
fun Reordering() {
    <Observe>
        val items = +state { listOf(1, 2, 3, 4, 5) }

        <LinearLayout orientation=LinearLayout.VERTICAL>
            items.value.forEachIndexed { index, id ->
                <Item
                    id
                    onMove={ amount ->
                        val next = index + amount
                        if (next >= 0 && next < items.value.size) {
                            items.value = items.value.move(index, index + amount)
                        }
                    }
                />
            }
        </LinearLayout>
    </Observe>
}

@Composable
private fun Item(@Pivotal id: Int, onMove: (Int) -> Unit) {
    <Observe>
        val count = +state { 0 }
        <LinearLayout orientation=LinearLayout.HORIZONTAL>
            <TextView text="id: $id amt: ${count.value}" textSize=20.sp />
            <Button text="+" onClick={ count.value++ } />
            <Button text="Up" onClick={ onMove(1) } />
            <Button text="Down" onClick={ onMove(-1) } />
        </LinearLayout>
    </Observe>
}

