package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.interfaces.BaseInterfaces
import com.adamhammer.ai_shimmer.interfaces.Memorize
import kotlinx.serialization.Serializable
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import java.util.concurrent.Future
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


interface AutonomousAIApi : BaseInterfaces {

}



// The BasicAgent class that exposes only the ideate method.
// It accepts a BasicAIApi instance (built with AiApiBuilder) in its constructor.
class AutonomousAgent(private val api: AutonomousAIApi) {
    fun step() : Future<AiDecision> {
        return api.decideNextAction()
        // next.execute(this or something)
    }
}

// Test class for BasicAgent.
class DecidingAgentTest {

    @Test
    fun testIdeate() {

        val api = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build()

        val agent = AutonomousAgent(api)
        val r1 = agent.step().get()
        val r2 = agent.step().get()
        print (r1);
        print (r2);

    }
}
