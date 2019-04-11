package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.ui.androidview.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.data.AuthenticationService

@Model
private class SignupModel(
        var username: String = "",
        var password: String = "",
        var isLoading: Boolean = false
)

@Composable
fun SignupScreen() {
    val model = +model { SignupModel() }
    val authentication = +ambient(AuthenticationService.Ambient)
    val navigator = +ambient(Ambients.NavController)
    val buttonEnabled = model.username.isNotEmpty() && model.password.isNotEmpty()

    fun onSubmit(authentication: AuthenticationService) {
        authentication.signup(model.username, model.password) { _, _ ->
            // Nothing to do
        }
        model.isLoading = true
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
                imageResource=R.drawable.reddit_verticallockup_onwhite />
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
                    imeOptions=EditorInfo.IME_ACTION_NEXT
                    controlledText=model.username
                    onTextChange={
                        model.username = it
                    }
//                        singleLine=true
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
                    imeOptions=EditorInfo.IME_ACTION_DONE
                    inputType=InputType.TYPE_TEXT_VARIATION_PASSWORD
                    controlledText=model.password
                    onTextChange={
                        model.password = it
                    }
//                          transformationMethod={PasswordTransformationMethod.getInstance()}
//                            singleLine=true
                    onEditorAction={ _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            onSubmit(authentication)
                        }
                        false
                    } />
            </TextInputLayout>
            <Button
                text="Sign Up"
                textSize=15.sp
                marginBottom=10.dp
                backgroundColor=Colors.PRIMARY
                textColor=Colors.TEXT_LIGHT
                enabled=buttonEnabled
                onClick={ onSubmit(authentication) } />
            <TextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                layoutGravity=Gravity.CENTER_HORIZONTAL
                padding=10.dp
                textAlignment=TextView.TEXT_ALIGNMENT_CENTER
                text="Already a member? Login."
                onClick={
                    navigator.navigate(R.id.nav_to_login)
                }
                textSize=15.sp
                textColor=Colors.TEXT_MUTED />
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