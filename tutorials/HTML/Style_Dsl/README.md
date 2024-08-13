# Style DSL

In this tutorial we have a look at how to style the components using the Style DSL. It’s a typesafe DSL for style sheets, which you can use to express CSS rules in your Kotlin code, and even modify styles based on the state of your Compose application.


### Inline Style

You can declare inline styles via the `style` block of a component

``` kotlin
Div({
    style {
        display(DisplayStyle.Flex)
        padding(20.px)
        
        // custom property (or not supported out of a box)
        property("font-family", "Arial, Helvetica, sans-serif")
    }
}) { /* content goes here */ }
```

In HTML, it will look like this:

```html
<div style="display: flex; padding: 20px; font-family: Arial, Helvetica, sans-serif;"></div>
```


### Stylesheet
An alternative way is to define a Stylesheet that contains rules:

``` kotlin
object AppStylesheet : StyleSheet() {
    val container by style { // container is a class
        display(DisplayStyle.Flex)
        padding(20.px)

        // custom property (or not supported out of a box)
        property("font-family", "Arial, Helvetica, sans-serif")
    }
}

// Stylesheet needs to be mounted:
renderComposable("root") {
    Style(AppStylesheet)
    
    Container {
        Text("Content")
    }
}

@Composable
fun Container(content: @Composable () -> Unit) {
    Div(
        attrs = { classes(AppStylesheet.container) }
    ) {
        content()
    }
}
```

In HTML, it will look like this:

```html
<style></style>
<div class="AppStylesheet-container">Content</div>
```

### Selectors examples

The Style DSL also provides a way to combine and unify selectors:

``` kotlin
object AppStylesheet : StyleSheet() {
    
    init {
        // `universal` can be used instead of "*": `universal style {}`
        "*" style { 
            fontSize(15.px)
            padding(0.px)
        }
        
        // raw selector
        "h1, h2, h3, h4, h5, h6" style {
            property("font-family", "Arial, Helvetica, sans-serif")
            
        }

        // combined selector
        type("A") + attr( // selects all tags <a> with href containing 'jetbrains'
            name = "href",
            value = "jetbrains",
            operator = CSSSelector.Attribute.Operator.Equals
        ) style {
            fontSize(25.px)
        }
    }
    
    // A convenient way to create a class selector
    // AppStylesheet.container can be used as a class in component attrs
    val container by style {
        color(Color.red)
        
        // hover selector for a class
        self + hover() style { // self is a selector for `container`
            color(Color.green)
        }
    }
}
```


### Media query example

To specify media queries, you can use the `media` function, which takes the related query, and a block of styles:

``` kotlin
object AppStylesheet : StyleSheet() {
    val container by style {
        padding(48.px)

        media(mediaMaxWidth(640.px)) {
            self style {
                padding(12.px)
            }
        }
    }
}
```

### CSS Variables

The style DSL also provides support for CSS variables.

``` kotlin
object MyVariables {
    // declare a variable
    val contentBackgroundColor by variable<CSSColorValue>()
}

object MyStyleSheet: StyleSheet() {
    
    val container by style {
        //set variable's value for the `container` scope
        MyVariables.contentBackgroundColor(Color("blue"))
    }
    
    val content by style {
        // get the value
        backgroundColor(MyVariables.contentBackgroundColor.value())
    }

    val contentWithDefaultBgColor by style {
        // default value can be provided as well
        // default value is used when the value is not previously set
        backgroundColor(MyVariables.contentBackgroundColor.value(Color("#333")))
    }
}
```


### Runnable example

```kotlin
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

object MyVariables {
    // declare a variable
    val contentBackgroundColor by variable<CSSColorValue>()
}

object MyStyleSheet: StyleSheet() {

    val container by style {
        //set variable's value for the `container` scope
        MyVariables.contentBackgroundColor(Color("blue"))
    }

    val content by style {
        // get the value
        backgroundColor(MyVariables.contentBackgroundColor.value())
    }

    val contentWithDefaultBgColor by style {
        // default value can be provided as well
        // default value is used when the value is not previously set
        backgroundColor(MyVariables.contentBackgroundColor.value(Color("#333")))
    }
}

object AppStylesheet : StyleSheet() {
    val container by style { // container is a class
        display(DisplayStyle.Flex)
        padding(20.px)

        // custom property (or not supported out of a box)
        property("font-family", "Arial, Helvetica, sans-serif")
    }
}

@Composable
fun Container(content: @Composable () -> Unit) {
    Div(
        attrs = { classes(AppStylesheet.container) }
    ) {
        content()
    }
}

fun main() {
    renderComposable(rootElementId = "root") {
        Div({
            style {
                display(DisplayStyle.Flex)
                padding(20.px)

                // custom property (or not supported out of a box)
                property("font-family", "Arial, Helvetica, sans-serif")
            }
        }) { /* content goes here */ }


        Style(AppStylesheet)

        Container {
            Text("Content")
        }
    }
}
```
