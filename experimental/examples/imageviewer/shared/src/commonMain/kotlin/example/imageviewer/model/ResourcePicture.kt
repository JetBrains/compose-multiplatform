package example.imageviewer.model

class ResourcePicture(
    val resource: String,
    override val name: String,
    override val description: String,
    override val geo: GeoPos
) : PictureData

val resourcePictures = listOf<PictureData>(
    ResourcePicture(
        resource = "1.jpg",
        name = "Taranaki",
        description = """
            Mount Taranaki is a dormant stratovolcano in the Taranaki region on the west coast of New Zealand's North Island.
            At 2,518 metres, it is the second highest mountain in the North Island, after Mount Ruapehu.
            It has a secondary cone, Fanthams Peak, 1,966 metres, on its south side.
        """.trimIndent(),
        geo = GeoPos(-39.296389, 174.064722)
    ),
    ResourcePicture(
        resource = "2.jpg",
        name = "Auckland SkyCity",
        description = """
            SkyCity Casino History
            This kiwi casino is a part of the Sky Tower, a giant resort that was completed in 1997.
            There were many New Zealand casinos at that time and this was in fact the second one ever built in the whole country
            """.trimIndent(),
        geo = GeoPos(-36.846589, 174.760871)
    ),
    ResourcePicture(
        resource = "3.jpg",
        name = "Empire State Building",
        description = """
            The Empire State Building is a 102-story Art Deco skyscraper in Midtown Manhattan, New York City.
             The building was designed by Shreve, Lamb & Harmon and built from 1930 to 1931. Its name is derived from "Empire State", the nickname of the state of New York.
            """.trimIndent(),
        geo = GeoPos(40.7473415, -73.9862414)
    ),
    ResourcePicture(
        resource = "4.jpg",
        name = "Tokyo Skytree",
        description = """
            Tokyo Skytree is a broadcasting and observation tower in Sumida, Tokyo.
            It became the tallest structure in Japan in 2010 and reached its full height of 634 meters in March 2011.
            """.trimIndent(),
        geo = GeoPos(35.7101, 139.8107)
    ),
    ResourcePicture(
        resource = "5.jpg",
        name = "Nakhal Fort",
        description = """
            Nakhal Fort is a large fortification in Al Batinah Region of Oman.
            It is named after the Wilayah of Nakhal.
            The fort houses a museum, operated by the Ministry of Tourism, which has exhibits of historic guns, and the fort also hosts a weekly goat market.
            """.trimIndent(),
        geo = GeoPos(23.395, 57.829)
    ),
    ResourcePicture(
        resource = "6.jpg",
        name = "Blue City",
        description = """
            Is a city in northwest Morocco.
            It is the chief town of the province of the same name and is noted for its buildings in shades of blue, for which it is nicknamed the "Blue City".
            Chefchaouen is situated just inland from Tangier and Tétouan.
            """.trimIndent(),
        geo = GeoPos(35.171389, -5.269722)
    ),
    ResourcePicture(
        resource = "7.jpg",
        name = "Berliner Fernsehturm",
        description = """
            At 368 meters, the Berlin television tower is the tallest building in Germany and the fifth tallest television tower in Europe.
            The television tower is located in the park at the television tower in Berlin's Mitte district.
            When it was completed in 1969, it was the second highest television tower in the world and, with over a million visitors a year, is one of the ten most popular sights in Germany.
            """.trimIndent(),
        geo = GeoPos(52.520833, 13.409444)
    ),
    ResourcePicture(
        resource = "8.jpg",
        name = "Hoggar Mountains",
        description = """
            The Hoggar Mountains are a highland region in the central Sahara in southern Algeria, along the Tropic of Cancer.
            The mountains cover an area of approximately 550,000 km.
            """.trimIndent(),
        geo = GeoPos(22.133333, 6.166667)
    ),
    ResourcePicture(
        resource = "9.jpg",
        name = "Botanic Garden",
        description = """
            Botanic Garden of Saint Petersburg State Forestry University.
            Forestry research hub with rare plants, flowers & trees amid wooded gardens & academy buildings.
            """.trimIndent(),
        geo = GeoPos(59.992327, 30.341782)
    ),
    ResourcePicture(
        resource = "10.jpg",
        name = "Mountain Ararat",
        description = """
            Mount Ararat is a snow-capped and dormant compound volcano in the extreme east of Turkey.
            It consists of two major volcanic cones: Greater Ararat and Little Ararat.
            Greater Ararat is the highest peak in Turkey and the Armenian Highland with an elevation of 5,137 m. 🏔
            """.trimIndent(),
        geo = GeoPos(40.169339, 44.488434)
    ),
    ResourcePicture(
        resource = "11.jpg",
        name = "Cabo da Roca",
        description = """
            The view on Cabo da Roca.
            Cabo da Roca or Cape Roca is a cape which forms the westernmost point of the Sintra Mountain Range, of mainland Portugal, of continental Europe, and of the Eurasian landmass.
            """.trimIndent(),
        geo = GeoPos(38.789283172, 9.4909725957)
    ),
    ResourcePicture(
        resource = "12.jpg",
        name = "Surprised Whiskers 🐱",
        description = """
            Surprised Whiskers: A Furry Tale.
            The photo captures Whiskers' adorably astonished expression as something unexpected catches his eye.
            The scene masterfully highlights the cat's vibrant fur and mesmerizing gaze, drawing the viewer into the furry tale unfolding before them.
            """.trimIndent(),
        geo = GeoPos(52.3560485, 4.9085645)
    ),
    ResourcePicture(
        resource = "13.jpg",
        name = "Software Engineering Donut",
        description = """
            Munich
            During our Introduction to Software Engineering Lectures, the professor would hand out little prizes to students who would solve coding challenges quickly.
            I solved a challenge about software design patterns as the first student of over 800, and got rewarded with this donut in the style of a cookie monster!
            It was really delicious! 😋
            """.trimIndent(),
        geo = GeoPos(48.1764708, 11.4580367)
    ),
    ResourcePicture(
        resource = "14.jpg",
        name = "Seligman Police Car.",
        description = """
            Seligman, USA
            I really enjoy old cars, and historic police cars are no exception! 🚓
            I stumbled across this one during a roadtrip across the united states in Seligman, a 500-soul town in the middle of the Arizona countryside.
            The extended hood and rounded forms of this car are just delightful to me. Plus, it has the option to go wee-ooo-wee-ooo! 🚨 
            """.trimIndent(),
        geo = GeoPos(35.3259364, -112.8553165)
    ),
    ResourcePicture(
        resource = "15.jpg",
        name = "Good Luck Charms",
        description = """
            Munich
            I decided I'd make my office a little bit more homely with trinkets from Tokyo and Las Vegas! 🐱🎰
            The cat is a variant of a Daruma doll, and is regarded more as a talisman of good luck, which you can never have enough of!
            The dice come from a casino in Las Vegas that shut down, and in traditional fashion, I decided they should show the numbers four and three, since that gives you the lucky number seven.
            These figures are still sitting on my desk, and it makes me really happy to look at them! 👀
            """.trimIndent(),
        geo = GeoPos(48.1458602, 11.5053059)
    ),
    ResourcePicture(
        resource = "16.jpg",
        name = "Pong Restaurant",
        description = """
            Stockholm, Sweden
            This little restaurant caught my eye because of the color scheme they use! 🦩
            The neon lights illuminating the dark streets of stockholm was a sight to behold, and the fact that only the first and last letter weren't lit up seems almost intentional.
            Also, the dumplings served at that place was delightful! 🥟
            """.trimIndent(),
        geo = GeoPos(59.3364318, 18.0587228)
    ),
)
