# Style DSL in Compose Web
**The API is experimental and breaking changes can be expected**

## What is covered
In this tutorial we have a look at how to style the components.


### Inline Style

```kotlin
Div(
    style = {
        display(DisplayStyle.Flex)
        padding(20.px)
        
        // custom property (or not supported out of a box)
        property("font-family", value("Arial, Helvetica, sans-serif"))
    }
) { /* content goes here */ }
```

In HTML, it will look like this:

```html
<div style="display: flex; padding: 20px; font-family: Arial, Helvetica, sans-serif;"></div>
```


### Stylesheet
An alternative way is to define a Stylesheet with rules:

```kotlin
object AppStylesheet : StyleSheet() {
    val container by style { // container is a class
        display(DisplayStyle.Flex)
        padding(20.px)

        // custom property (or not supported out of a box)
        property("font-family", value("Arial, Helvetica, sans-serif"))
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

```kotlin
object AppStylesheet : StyleSheet() {
    
    init {
        // CSSSelector.Universal can be used instead of "*"
        "*" style { 
            fontSize(15.px)
            padding(0.px)
        }
        
        // raw selector
        "h1, h2, h3, h4, h5, h6" style {
            property("font-family", value("Arial, Helvetica, sans-serif"))
            
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
    // AppStylesheet.container can be used a class name in components
    val container by style {
        color("red")
        
        // hover selector for a class
        self + hover() style { // self is a selector for `container`
            color("green")
        }
    }
}
```


### Media query example

```kotlin
object AppStylesheet : StyleSheet() {
    val container by style {
        padding(48.px)

        media(maxWidth(640.px)) {
            self style {
                padding(12.px)
            }
        }
    }
}
```

### CSS Variables

```kotlin
object MyVariables : CSSVariables {
    // declare a variable
    val contentBackgroundColor by variable<Color>() 
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