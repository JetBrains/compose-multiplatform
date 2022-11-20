//import androidx.compose.ui.test.InternalTestApi
//import androidx.compose.ui.test.junit4.createComposeRule
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.runBlocking
//import org.jetbrains.compose.resources.ExperimentalResourceApi
//import org.jetbrains.compose.resources.resource
//import org.junit.Assert
//import org.junit.Assert.assertEquals
//import org.junit.Rule
//import org.junit.Test
//
//@OptIn(InternalTestApi::class, ExperimentalResourceApi::class)
//class SimpleTest {
//
//    @get:Rule
//    val rule = createComposeRule()
//
//    @Test
//    fun testSameResource() {
//        Assert.assertEquals(1,2)
//        runBlocking (Dispatchers.Main) {
//            rule.setContent {
////                assertEquals(resource("img.png"), resource("img1"))
//            }
//        }
//    }
//
//}
