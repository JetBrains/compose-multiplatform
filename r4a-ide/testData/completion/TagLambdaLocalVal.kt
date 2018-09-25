import com.google.r4a.*
import android.widget.TextView

fun f(){
    val abc = @Composable { <TextView /> }
    <<caret>
}

// EXIST: { lookupString: "abc", itemText: "<abc />" }

