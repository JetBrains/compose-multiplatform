import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import com.example.compose.common.uikit.UIKitApplier
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSCoder
import platform.UIKit.UIViewController

@ExportObjCClass
class ComposeViewController : UIViewController {
    @OverrideInit
    constructor() : super(nibName = null, bundle = null)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    private val job = Job()
    private val dispatcher = Dispatchers.Main
    private val frameClock = BroadcastFrameClock(onNewAwaiters = { })
    private val coroutineScope = CoroutineScope(job + dispatcher + frameClock)
    private val renderCoroutineScope = CoroutineScope(Dispatchers.Main)

    private lateinit var recomposer: Recomposer
    private lateinit var composition: Composition

    private lateinit var content: @Composable () -> Unit

    override fun viewDidLoad() {
        super.viewDidLoad()

        // useful links
        // https://github.com/JakeWharton/mosaic/blob/4cb027c2074d86b3389cbfb5da35468fe7591178/mosaic/mosaic-runtime/src/main/kotlin/com/jakewharton/mosaic/mosaic.kt
        // https://github.com/JetBrains/androidx/blob/3ebf1d446089cc8da34bb1b906982327af7b8249/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/ComposeScene.skiko.kt

        recomposer = Recomposer(coroutineScope.coroutineContext)
        composition = Composition(UIKitApplier(this.view), recomposer)

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }

        composition.setContent(content)

        renderCoroutineScope.launch {
            while (isActive) {
                frameClock.sendFrame(0L) // Frame time value is not used by Compose runtime.
                delay(50)
            }
        }

        Snapshot.registerGlobalWriteObserver {
            coroutineScope.launch {
                Snapshot.sendApplyNotifications()
            }
        }
    }

    override fun viewDidUnload() {
        super.viewDidUnload()

        renderCoroutineScope.cancel()
        composition.dispose()
        recomposer.cancel()
        job.cancel()
    }

    fun setContent(content: @Composable () -> Unit) {
        this.content = content
    }
}
