import android.graphics.Color
import android.widget.*
import com.google.r4a.*

class MainComponent : Component() {

    public override fun render() {
        if(false)
            <MyTag>
                    println("do something");
            </MyTag>
        else
            <MyTag>
                    println("do something");
            </MyTag>
    }

    public override fun render() {
        while(false)
            <MyTag>
                    println("do something");
            </MyTag>
    }

    public override fun render() {
        for(x in 1...5)
            <MyTag>
                println("do something");
            </MyTag>
    }

    public override fun render() {
        val myArg: Any = 8
        val arg1 = myArg as Array<Int>
        <foo myArg />
        val arg = myArg as Int
        <foo arg />
    }
}
