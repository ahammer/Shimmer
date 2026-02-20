import androidx.compose.material.MaterialTheme
import com.adamhammer.shimmer.samples.dnd.compose.ShimmerDndApp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Shimmer D&D") {
        MaterialTheme {
            ShimmerDndApp()
        }
    }
}
