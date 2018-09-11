import android.graphics.Color
import android.widget.*
import com.google.r4a.*

class MainComponent : Component() {

    public override fun render() {
        <LinearLayout>
            <FancyComponent1> value ->
                <TextView text=value />
            </FancyComponent1>
            <FancyComponent2> p1, p2 ->
                <TextView text=(p1+p2) />
            </FancyComponent2>
            <FancyComponent2> (p1, p2) ->
                <TextView text=(p1+p2) />
            </FancyComponent2>
            <FancyComponent3> p1: String, p2: Int, p3: ArrayList<String> ->
                <TextView text=(p1+p2+p3) />
            </FancyComponent3>
            <FancyComponent3> (p1: String, p2: Int, p3: ArrayList<String>) ->
                <TextView text=(p1+p2+p3) />
            </FancyComponent3>

            if(true) // Use `if` to assert that it also works with the "block" style parsing
                <FancyComponent1> value ->
                    <TextView text=value />
                </FancyComponent1>
    }
}
