# Module root

Compose Material

# Package androidx.compose.material

Build Jetpack Compose UIs with ready to use Material Design Components. This is the higher level entry point of Compose, designed to provide components that match those described at <a href="https://material.io" class="external" target="_blank">material.io</a>.

![Material Design image](https://developer.android.com/images/reference/androidx/compose/material/material-design.png)

In this page, you'll find documentation for types, properties, and functions available in the `androidx.compose.material` package.

For more information, check out the <a href="https://developer.android.com/jetpack/compose/themes/material" class="external" target="_blank">Material Theming in Compose</a> guide.

## Overview

### Theming

<a href="https://material.io/design/material-theming/overview.html" class="external" target="_blank">Material Theming</a> refers to the customization of your Material Design app to better reflect your product’s brand.

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Material Theming** | [MaterialTheme] | Material Theme |
| **Color** | [Colors] | Material Design color system |
| **Typography** | [Typography] | Material Design type scale |
| **Shape** | [Shapes] | Material Design shape |

### Components

<a href="https://material.io/components" class="external" target="_blank">Material Components</a> are interactive building blocks for creating a user interface.

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **App bars: bottom** | [BottomAppBar] | Bottom app bar |
| **App bars: top** | [TopAppBar] | Top app bar |
| **Backdrop** | [BackdropScaffold] | Backdrop |
| **Bottom navigation** | [BottomNavigation] | Bottom navigation |
| **Buttons** | [Button] | Contained button |
|  | [OutlinedButton] | Outlined button |
|  | [TextButton] | Text button |
| **Buttons: floating action button** | [FloatingActionButton] | Floating action button |
|  | [ExtendedFloatingActionButton] | Extended floating action button |
| **Cards** | [Card] | Card |
| **Checkboxes** | [Checkbox] | Checkbox |
|  | [TriStateCheckbox] | Parent checkbox |
| **Dialogs** | [AlertDialog] | Alert dialog |
| **Dividers** | [Divider] | Divider |
| **Lists** | [ListItem] | List item |
| **Menus** | [DropdownMenu] | Dropdown menu |
|  | [DropdownMenuItem] | Dropdown menu item |
| **Navigation drawer** | [ModalDrawer] | Modal navigation drawer |
|  | [BottomDrawer] | Bottom navigation drawer |
| **Progress indicators** | [LinearProgressIndicator] | Linear progress indicator |
|  | [CircularProgressIndicator] | Circular progress indicator |
| **Radio buttons** | [RadioButton] | Radio button |
| **Sheets: bottom** | [BottomSheetScaffold] | Standard bottom sheet |
|  | [ModalBottomSheetLayout] | Modal bottom sheet |
| **Sliders** | [Slider] | Slider |
| **Snackbars** | [Snackbar] | Snackbar |
| **Switches** | [Switch] | Switch |
| **Tabs** | [Tab] | Tab |
|  | [LeadingIconTab] | Icon tab |
|  | [TabRow] | Fixed tabs |
|  | [ScrollableTabRow] | Scrollable tabs |
| **Text fields** | [TextField] | Filled text field |
|  | [OutlinedTextField] | Outlined text field |

### Dark theme

A <a href="https://material.io/design/color/dark-theme.html" class="external" target="_blank">dark theme</a> is a low-light UI that displays mostly dark surfaces.

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **System** | [androidx.compose.foundation.isSystemInDarkTheme] | System dark theme |
| **Elevation** | [ElevationOverlay] | Elevation overlay |
| **Color** | [primarySurface] | Primary surface color |

### Surfaces and layout

Material Design defines the qualities that can be expressed by UI regions, surfaces, and components.

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Surfaces** | [Surface] | Material surface |
| **Layout** | [Scaffold] | Basic Material Design visual layout structure |

### Icons

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Icon** | [Icon] | Icon |
| **Icon button** | [IconButton] | Icon button |
| **Icon toggle button** | [IconToggleButton] | Icon toggle button |

Also check out the `androidx.compose.material.icons` [package](/reference/kotlin/androidx/compose/material/icons/package-summary).
