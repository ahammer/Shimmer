import com.adamhammer.ai_shimmer.interfaces.ApiAdapter

abstract class BaseApiAdapter: ApiAdapter {
    private val memoryMap: MutableMap<String, String> = mutableMapOf()
    override fun getMemoryMap() = memoryMap
}