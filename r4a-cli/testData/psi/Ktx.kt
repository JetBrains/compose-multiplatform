import android.graphics.Color
import android.widget.*
import com.google.r4a.*

class MainComponent : Component() {

    public override fun render() {
        val fontSize = MyApplicationData.currentFontSize;
        <LinearLayout orientation="vertical" layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)>
            <android.widget.Button text="Increase Font Size" onClick={MyApplicationData.currentFontSize = fontSize+5; rerender()} enabled=(fontSize < 100) />
            <android.widget.Button text="Decrease Font Size" onClick={MyApplicationData.currentFontSize = fontSize-5; rerender()} enabled=(fontSize > 10) />
            <LinearLayout>
                var colorButtonLayout = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                val enabled = true
                <Button text="Red" layoutParams=colorButtonLayout onClick={MyApplicationData.currentColor = Color.RED; rerender()} />
                <Button text="Green" layoutParams=colorButtonLayout onClick={MyApplicationData.currentColor = Color.GREEN; rerender()} />
                <Button text="Blue" layoutParams=colorButtonLayout onClick={MyApplicationData.currentColor = Color.BLUE; rerender()} />
                <Button text="Gray" layoutParams=colorButtonLayout onClick={MyApplicationData.currentColor = Color.GRAY; rerender()} />
                <Button enabled />
            </LinearLayout>
            <ScrollView>
                <LinearLayout orientation="vertical">
                    for(x in 1..25) {
                        <TextView text = ("Hello Stephanie "+x+"!") textSize = fontSize textColor=MyApplicationData.currentColor />
                    }
                </LinearLayout>
            </ScrollView>
        </LinearLayout>
    }
}