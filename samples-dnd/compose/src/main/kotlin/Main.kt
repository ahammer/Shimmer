import com.adamhammer.shimmer.samples.dnd.compose.ShimmerDndAppV2
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Shimmer D&D") {
        ShimmerDndAppV2()
    }
}
