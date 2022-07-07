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

package androidx.compose.foundation.demos.text

private val LoremIpsumWords = """
    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque a egestas nisi. Aenean
    aliquam neque lacus, ac sollicitudin risus consequat in. Pellentesque habitant morbi tristique
    senectus et netus et malesuada fames ac turpis egestas. In id nulla quam. Ut lobortis justo
    purus, nec viverra diam gravida nec. Duis scelerisque feugiat ante, vitae semper leo. Donec
    tempor aliquet lacus rhoncus tristique. In pulvinar sem ac velit suscipit fermentum. Donec
    quis dolor ut enim sollicitudin laoreet. Morbi ac lectus pharetra, condimentum lectus
    congue, euismod ante.

    Phasellus vitae varius mi. Pellentesque non lacus at erat posuere pharetra a nec lorem. Vivamus
    posuere massa sed ultricies euismod. Fusce luctus tristique diam at luctus. Nulla efficitur
    fermentum dolor, nec hendrerit diam rhoncus ut. Donec fermentum, enim ultrices pharetra
    suscipit, ex ligula iaculis ligula, in aliquam erat felis id magna. In est dolor, consequat
    sed eros non, varius consequat velit. Cras blandit urna et vulputate dapibus. Donec
    efficitur laoreet condimentum. Pellentesque habitant morbi tristique senectus et netus et
    malesuada fames ac turpis egestas. Nullam scelerisque vitae est id pellentesque.

    Aenean in ipsum rhoncus, dignissim lorem ac, consectetur quam. Phasellus id velit nec velit
    dictum eleifend at eu nibh. Integer feugiat turpis quis dui pharetra, suscipit ornare turpis
    aliquam. Phasellus tristique ullamcorper placerat. Aenean aliquet maximus tortor eu posuere.
    Curabitur semper libero ut libero lacinia tempor. Duis tempor mattis nulla in malesuada.
    Mauris rutrum commodo lacus. Integer pellentesque odio at eleifend vulputate. Donec eu erat
    ut neque porttitor vehicula.

    Duis est nisl, consequat sed ipsum eget, cursus auctor mauris. Vestibulum mattis sem et molestie
    blandit. Donec tincidunt enim nibh, nec cursus magna feugiat id. Nulla facilisi. Suspendisse
    ultricies lobortis ex, at vestibulum risus. Aliquam congue viverra justo vitae mollis. In
    elementum venenatis eros non tincidunt. Pellentesque vitae sem dui. Quisque magna lacus,
    dignissim sed viverra ut, sagittis quis turpis.

    Aenean cursus id dolor sed convallis. Interdum et malesuada fames ac ante ipsum primis in
    faucibus. Cras sodales ante et rhoncus commodo. In hac habitasse platea dictumst. Aenean
    efficitur felis in elementum sollicitudin. In neque urna, tincidunt et lorem nec, commodo
    maximus urna. Suspendisse tincidunt, felis ac viverra ultrices, nulla ipsum ornare nisi,
    eleifend luctus tellus felis nec orci. Maecenas sed venenatis urna. Nulla tempor ultricies
    lorem eget finibus. Nunc malesuada nisi id volutpat lobortis. Sed aliquet massa eu ex
    eleifend efficitur. Curabitur accumsan vestibulum ligula sed aliquet. Nulla pretium dui id
    nunc ultricies, id porttitor lorem pretium. Lorem ipsum dolor sit amet, consectetur
    adipiscing elit.
""".trimIndent().split("""\s""".toRegex())

fun loremIpsum(wordCount: Int = LoremIpsumWords.size): String =
    LoremIpsumWords.joinToString(separator = " ", limit = wordCount)