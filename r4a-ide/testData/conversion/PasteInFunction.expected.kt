import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.adapters.setLayoutHeight
import com.google.r4a.adapters.setLayoutWidth

@Composable
fun MyComponent() {
    <TextView
        layoutWidth=WRAP_CONTENT
        layoutHeight=WRAP_CONTENT
        text="Hello World!" />
}