/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.animation.demos.lookahead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun ScreenSizeChangeDemo() {
    // A surface container using the 'background' color from the theme
    var state by remember { mutableStateOf(DisplayState.Tablet) }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            state =
                if (state == DisplayState.Tablet) DisplayState.Compact else DisplayState.Tablet
        },
        contentAlignment = Alignment.TopStart
    ) {
        Root(state)
    }
}

// Simulate different state
enum class DisplayState {
    Compact,
    Tablet
}

@Composable
fun SceneScope.List(modifier: Modifier) {
    Column(modifier) {
        SearchBar()
        Card(cardData = MessageList[0])
        Card(cardData = MessageList[1], true)
        Card(cardData = MessageList[2])
    }
}

@Composable
fun SceneScope.Details(modifier: Modifier) {
    Surface(shape = RoundedCornerShape(5.dp), modifier = modifier, color = Color(0xfff7f2fa)) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Dinner Club", fontSize = 20.sp)
                    Text("3 Messages", fontSize = 10.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Delete,
                    modifier = Modifier.padding(2.dp)
                        .background(Color.White, RoundedCornerShape(3.dp)).padding(6.dp),
                    contentDescription = null
                )
                Icon(
                    Icons.Default.Menu,
                    modifier = Modifier.padding(2.dp)
                        .background(Color.White, RoundedCornerShape(3.dp)).padding(6.dp),
                    contentDescription = null
                )
            }
            Message(MessageList[1])
            Message(MessageList[3])
        }
    }
}

@Composable
fun Root(state: DisplayState) {
    SceneHost {
        Row(
            if (state == DisplayState.Compact) {
                Modifier.requiredWidth(800.dp).fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }.sharedElement()
                .background(Color(0xffeae7f2))
                .padding(top = 10.dp, start = 10.dp, end = 10.dp)
        ) {
            NavRail(state)
            List(modifier = Modifier.weight(3f))
            Spacer(Modifier.size(10.dp))
            Details(modifier = Modifier.weight(4f))
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

data class MessageData(
    val name: String,
    val time: String,
    val subject: String,
    val content: String,
    val addressing: String
)

val MessageList = listOf<MessageData>(
    MessageData(
        "老强",
        "10 mins ago",
        "豆花鱼",
        "最近忙吗？昨晚我去了你最爱的那家饭馆，点了他们特色的豆花鱼，吃着吃着就想你了。有空咱们视频？",
        ""
    ),
    MessageData(
        "So Duri",
        "20 mins ago",
        "Dinner Club",
        "I think it's time for us to finally try that new noodle shop downtown that doesn't use" +
            " menus. Anyone else have other suggestions for dinner club this week? I'm so" +
            "intrigued by this idea of a noodle restaurant where no one gets to order for" +
            "themselves - could be fun, or terrible, or both: :) \n\n" +
            "So ",
        "To me, Ziad and Lily"
    ),
    MessageData(
        "Lily",
        "2 hours ago",
        "This food show is made for you",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor" +
            " incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo" +
            " consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse" +
            " cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non" +
            " proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        ""
    ),
    MessageData(
        "Me",
        "4 mins ago",
        "",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor" +
            " incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo" +
            " consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse" +
            " cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non" +
            " proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        "To me, Ziad and Lily"
    )
)

@Composable
fun SearchBar() {
    Surface(
        shape = RoundedCornerShape(40),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.padding(10.dp)
            )
            Text("Search", color = Color.LightGray, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .size(35.dp)
                    .background(Color(0xffecddff), CircleShape)
            )
        }
    }
}

@Composable
fun Header(data: MessageData) {
    Row(
        Modifier
            .height(60.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(10.dp)
                .size(35.dp)
                .background(Color(0xffffddee), CircleShape)
        )
        Column(Modifier.weight(1f)) {
            Text(data.name, color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Text(data.time, color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
        Icon(
            Icons.Outlined.Star,
            contentDescription = null,
            Modifier
                .background(Color.White, CircleShape)
                .padding(10.dp)
        )
    }
}

@Composable
fun SceneScope.Card(cardData: MessageData, selected: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.padding(2.dp),
        color = if (selected) Color(0xffecddff) else Color(0xfff3edf7)
    ) {
        Column(Modifier.padding(10.dp)) {
            Header(data = cardData)
            Text(
                cardData.subject,
                fontSize = 19.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                cardData.content,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 10.dp).animateSizeAndSkipToFinalLayout()
            )
        }
    }
}

@Composable
fun SceneScope.Message(messageData: MessageData) {
    Column(
        Modifier
            .padding(5.dp)
            .background(Color(0xfffffbfe), RoundedCornerShape(5.dp))
            .padding(10.dp)
    ) {
        Header(data = messageData)
        Text(
            messageData.addressing,
            fontSize = 12.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(start = 10.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            messageData.content,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 10.dp).animateSizeAndSkipToFinalLayout()
        )
        Spacer(Modifier.size(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .height(40.dp)
                    .background(Color(0xfff2ecf6), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("Reply")
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .height(40.dp)
                    .background(Color(0xfff2ecf6), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("Reply All")
            }
        }
    }
}

@Composable
fun SceneScope.NavRail(state: DisplayState) {
    Column(
        Modifier
            .then(
                if (state == DisplayState.Tablet) Modifier.width(200.dp) else Modifier.width(
                    IntrinsicSize.Min
                )
            )
            .padding(top = 20.dp, end = 5.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth().animateSizeAndSkipToFinalLayout()
                .padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (state == DisplayState.Tablet) {
                Text("REPLY", color = Color(0xffa493c5), fontSize = 15.sp, letterSpacing = 0.12.em)
            }
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.width(40.dp).sharedElement()
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .background(Color(0xffffddee), RoundedCornerShape(8.dp))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Create,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.width(40.dp)
            )
            if (state == DisplayState.Tablet) {
                Text(
                    "Compose",
                    Modifier.padding(start = 30.dp),
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.size(20.dp))
        Item(state, Icons.Outlined.Email, "Inbox", Color(0xffe8def8))
        Item(state, Icons.Outlined.List, "Articles")
        Item(state, Icons.Default.Send, "Direct Messages")
        Item(state, Icons.Filled.Notifications, "Video Chat")
    }
}

@Composable
fun Item(state: DisplayState, icon: ImageVector, text: String, color: Color = Color.Transparent) {
    Row(
        Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(color, RoundedCornerShape(50))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, contentDescription = null, tint = Color.Gray,
            modifier = Modifier.width(40.dp)
        )
        if (state == DisplayState.Tablet) {
            Text(
                text,
                Modifier
                    .weight(1f)
                    .padding(start = 15.dp),
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
