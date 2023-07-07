// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.integration.docs.tutorial

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/tutorial
 *
 * No action required if it's modified.
 *
 * Tech writers: on DAC, these snippets contain html formatting that is omitted here.
 */

private object TutorialSnippet1 {
    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Text("Hello world!")
            }
        }
    }
}

/*
Page 2
 */

private object TutorialSnippet2 {
    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Greeting("Android")
            }
        }
    }

    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }
}

/*
Page 3
 */

private object TutorialSnippet3 {
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }

    @Preview
    @Composable
    fun PreviewGreeting() {
        Greeting("Android")
    }
}

/*
Lesson 2
 */

private object TutorialSnippet4 {
    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                NewsStory()
            }
        }
    }

    @Composable
    fun NewsStory() {
        Text("A day in Shark Fin Cove")
        Text("Davenport, California")
        Text("December 2018")
    }

    @Preview
    @Composable
    fun DefaultPreview() {
        NewsStory()
    }
}

/*
Page 2
 */

private object TutorialSnippet5 {
    @Composable
    fun NewsStory() {
        Column {
            Text("A day in Shark Fin Cove")
            Text("Davenport, California")
            Text("December 2018")
        }
    }
}

private object TutorialSnippet6 {
    @Composable
    fun NewsStory() {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("A day in Shark Fin Cove")
            Text("Davenport, California")
            Text("December 2018")
        }
    }
}

private object TutorialSnippet7 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Image(image, contentDescription = null)

            Text("A day in Shark Fin Cove")
            Text("Davenport, California")
            Text("December 2018")
        }
    }
}

private object TutorialSnippet8 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val imageModifier = Modifier
                .height(180.dp)
                .fillMaxWidth()

            Image(
                image,
                contentDescription = null,
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )

            Text("A day in Shark Fin Cove")
            Text("Davenport, California")
            Text("December 2018")
        }
    }
}

/*
Lesson 3
 */
private object TutorialSnippet9 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val imageModifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(4.dp))

            Image(
                image,
                contentDescription = null,
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))

            Text("A day in Shark Fin Cove")
            Text("Davenport, California")
            Text("December 2018")
        }
    }
}

private object TutorialSnippet10 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)
        MaterialTheme {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val imageModifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(4.dp))

                Image(
                    image,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))

                Text("A day in Shark Fin Cove")
                Text("Davenport, California")
                Text("December 2018")
            }
        }
    }
}

private object TutorialSnippet11 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)
        MaterialTheme {
            val typography = MaterialTheme.typography
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val imageModifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(4.dp))

                Image(
                    image,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))

                Text("A day in Shark Fin Cove",
                    style = typography.h6)
                Text("Davenport, California",
                    style = typography.body2)
                Text("December 2018",
                    style = typography.body2)
            }
        }
    }
}

private object TutorialSnippet12 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)
        MaterialTheme {
            val typography = MaterialTheme.typography
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val imageModifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(4.dp))

                Image(
                    image,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    "A day wandering through the sandhills " +
                        "in Shark Fin Cove, and a few of the " +
                        "sights I saw",
                    style = typography.h6)
                Text("Davenport, California",
                    style = typography.body2)
                Text("December 2018",
                    style = typography.body2)
            }
        }
    }
}

/* ktlint-disable indent */
private object TutorialSnippet13 {
    @Composable
    fun NewsStory() {
        val image = painterResource(R.drawable.header)
        MaterialTheme {
            val typography = MaterialTheme.typography
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val imageModifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(4.dp))

                Image(
                    image, null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    "A day wandering through the sandhills " +
                        "in Shark Fin Cove, and a few of the " +
                        "sights I saw",
                    style = typography.h6,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text("Davenport, California",
                    style = typography.body2)
                Text("December 2018",
                    style = typography.body2)
            }
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object drawable {
        const val header = 1
    }
}
