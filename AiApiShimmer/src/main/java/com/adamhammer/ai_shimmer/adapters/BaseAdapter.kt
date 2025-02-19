import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import java.util.concurrent.Future

abstract class BaseApiAdapter<T>: ApiAdapter<T> {
    override fun decideNextAction(): Future<AiDecision> {
        TODO("Not yet implemented")
    }

    override fun add(key: String, description: String, function: (String) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun add(key: String, description: String, input: String) {
        TODO("Not yet implemented")
    }

    override fun runAction(decision: AiDecision) {
        TODO("Not yet implemented")
    }

    private val memoryMap: MutableMap<String, String> = mutableMapOf()
    override fun getMemoryMap() = memoryMap
}