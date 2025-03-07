package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.StubAdapter
import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.annotations.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

// Import new custom annotations

import org.junit.jupiter.api.Assertions.*

class AiApiShimmerTest {

    @Serializable
    @AiSchema(title = "Question", description = "Holds info about the question")
    class Question(
        @field:AiSchema(title = "Question", description = "The question to be asked")
        val question: String = "",
        @field:AiSchema(title = "Context", description = "Who is asking the Question")
        val context: String = ""
    )

    @Serializable
    @AiSchema(title = "The Answer", description = "Holds the answer to the question.")
    class Answer(
        @field:AiSchema(title = "Answer", description = "A resoundingly deep answer to the question")
        val answer: String = ""
    )

    interface QuestionAPI {
        @AiOperation(
            summary = "Ask",
            description = "Provide an in-depth answer to the question within its context."
        )
        @AiResponse(
            description = "The answer to the question",
            responseClass = Answer::class
        )
        fun askStruct(
            @AiParameter(description = "The question and its context for the API call")
            question: Question?
        ): Future<Answer?>

        @AiOperation(
            summary = "AskString",
            description = "Provide an in-depth answer to the question within its context, returning a string response."
        )
        @AiResponse(
            description = "The answer as a string",
            responseClass = String::class
        )
        @Memorize("The last answer to the question.")
        fun askString(
            @AiParameter(description = "The question and its context for the API call")
            question: Question?
        ): Future<String?>
    }

    @Test
    fun testStubApi() {
        val api = ShimmerBuilder(QuestionAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val result = api.askStruct(Question("What is the meaning of life", "A curious student"))
        val answer = result.get()
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    fun testJsonApi() {
        val answer = ShimmerBuilder(QuestionAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api
            .askStruct(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer?.answer)
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    fun testStringApi() {
        val answer = ShimmerBuilder(QuestionAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api
            .askString(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer)
        assertNotNull(answer, "There is no answer for the shimmer")
        // assertEquals(adapter.getMemoryMap().size, 1, "we should have one memory")
    }
}
