import com.adamhammer.ai_shimmer.ShimmerBuilder
import com.adamhammer.ai_shimmer.agents.DecidingAgent
import com.adamhammer.ai_shimmer.agents.DecidingAgentAPI
import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import java.util.concurrent.Future

abstract class BaseApiAdapter: ApiAdapter {

    private val decidingAgent by lazy {
        DecidingAgent(api = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapter(this)
            .build())
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