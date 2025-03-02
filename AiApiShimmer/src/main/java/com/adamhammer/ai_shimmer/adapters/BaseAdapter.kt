import com.adamhammer.ai_shimmer.ShimmerBuilder
import com.adamhammer.ai_shimmer.agents.DecidingAgent
import com.adamhammer.ai_shimmer.agents.DecidingAgentAPI
import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.reflect.KClass

abstract class BaseApiAdapter(private val baseType: KClass<Any>): ApiAdapter {

    private val decidingAgent by lazy {
        DecidingAgent(api = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(this)
            .build())
    }

    override fun getBaseType(): KClass<Any> {
        return baseType
    }

    override fun decideNextAction(): Future<AiDecision> {
            return decidingAgent.decideNext(this)
    }

    override fun runAction(decision: AiDecision) {
        TODO("Not yet implemented")
    }

    private val memoryMap: MutableMap<String, String> = mutableMapOf()
    override fun getMemoryMap() = memoryMap
}