package com.google.r4a.examples.explorerapp.ui.screens

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.*
import androidx.ui.androidview.adapters.*

@Model
class Counter {
    var count: Int = 0
        private set

    fun next() {
        count++
    }
}


@Model
class ExampleModel {
    val buttonCounter = Counter()
}

@Composable
fun CounterView(counter: Counter) {
    <Observe>
        <LinearLayout>
             <TextView text="Count=${counter.count}" />
        </LinearLayout>
    </Observe>
}

var recomposeCount = 0

@Composable
fun ModelExample(model: ExampleModel = remember { ExampleModel() }) {
    <Observe>
        <LinearLayout>
            <TextView text="Recompose count=${recomposeCount++}" />
            <CounterView counter=model.buttonCounter />
            <Button text="Press me" onClick={ model.buttonCounter.next() } />
        </LinearLayout>
    </Observe>
}

