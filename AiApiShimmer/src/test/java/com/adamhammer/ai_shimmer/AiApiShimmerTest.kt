package com.adamhammer.ai_shimmer

import StubAdapter
import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

class AiApiShimmerTest {

    @Serializable
    @AI(label = "Question", description = "Holds info about the question")
    class Question(
        @property:AI(label = "Question", description = "The question to be asked")
        val text: String = "",
        @property:AI(label = "Context", description = "Who is asking the Question")
        val context: String = ""
    )

    @Serializable
    @AI(label = "The Answer", description = "Holds the answer to the question.")
    class Answer(
        @property:AI(label = "Answer", description = "The text field holding the answer.")
        val text: String = ""
    )

    interface QuestionAPI {
        @AI(label = "Ask", description = "Ask a funny Question")
        fun ask(
            question: Question?
        ): Future<Answer?>
    }

    @Test
    public fun testStubApi() {
        val api = AiApiBuilder(QuestionAPI::class)
            .setAdapter(StubAdapter())
            .build();

        val result = api.ask(Question("What is the meaning of life", "A curious student"))
        val answer = result.get()
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    public fun testRealApi() {
        val api = AiApiBuilder(QuestionAPI::class)
            .setAdapter(OpenAiAdapter())
            .build();

        val result = api.ask(Question("What is the meaning of life", "A curious student"))
        val answer = result.get()
        assertNotNull(answer, "There is no answer for the shimmer")
    }
}