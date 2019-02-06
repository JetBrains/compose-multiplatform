package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import com.google.r4a.Component
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import com.google.r4a.*


@Composable
fun LoginScreen() {
    val username = +state { "" }
    val password = +state { "" }

    val navigator = +ambient(Ambients.NavController)
    val buttonEnabled = username.value.isNotEmpty() && password.value.isNotEmpty()

    fun onSubmit() {

    }

    <ScrollView
        layoutWidth=MATCH_PARENT
        layoutHeight=MATCH_PARENT
    >
        <LinearLayout
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
            padding=24.dp
            orientation=LinearLayout.VERTICAL
        >
            <ImageView
                layoutWidth=160.dp
                layoutHeight=160.dp
                layoutGravity=Gravity.CENTER_HORIZONTAL
                marginBottom=24.dp
                imageResource=R.drawable.reddit_verticallockup_onwhite
            />
            <TextInputLayout
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                layoutGravity=Gravity.CENTER_HORIZONTAL
            >
                <EditText
                    paddingHorizontal=16.dp
                    paddingVertical=16.dp
                    textSize=15.sp
                    hint="Username"
                    controlledText=username.value
                    onTextChange={
                        username.value = it
                    }
                    singleLine=true
                    imeOptions=EditorInfo.IME_ACTION_NEXT
                />
            </TextInputLayout>
            <TextInputLayout
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                layoutGravity=Gravity.CENTER_HORIZONTAL
                passwordVisibilityToggleEnabled=true
            >
                <TextInputEditText
                    paddingHorizontal=16.dp
                    paddingVertical=16.dp
                    textSize=15.sp
                    hint="Password"
                    controlledText=password.value
                    onTextChange={
                        password.value = it
                    }
//                        transformationMethod={PasswordTransformationMethod.getInstance()}
                    singleLine=true
                    imeOptions=EditorInfo.IME_ACTION_DONE
                    inputType=InputType.TYPE_TEXT_VARIATION_PASSWORD
                    onEditorAction={ view, actionId, key ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            onSubmit()
                        }
                        false
                    }
                />
            </TextInputLayout>
            <Button
                text="Sign Up"
                textSize=15.sp
                marginBottom=10.dp
                backgroundColor=Colors.PRIMARY
                textColor=Colors.TEXT_LIGHT
                enabled=buttonEnabled
                onClick={
                    onSubmit()
                }
            />
            <TextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                layoutGravity=Gravity.CENTER_HORIZONTAL
                padding=10.dp
                textAlignment=TextView.TEXT_ALIGNMENT_CENTER
                text="No account yet? Create one."
                onClick={
                    navigator.navigate(R.id.nav_to_signup)
                }
                textSize=15.sp
                textColor=Colors.TEXT_MUTED />
//                      text="No account yet? Create one."
            <TextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                layoutGravity=Gravity.CENTER_HORIZONTAL
                padding=10.dp
                textAlignment=TextView.TEXT_ALIGNMENT_CENTER
                text="Use app without logging in."
                onClick={
                    navigator.navigate(R.id.screen_link_list)
                }
                textSize=15.sp
                textColor=Colors.TEXT_MUTED />
        </LinearLayout>
    </ScrollView>
}