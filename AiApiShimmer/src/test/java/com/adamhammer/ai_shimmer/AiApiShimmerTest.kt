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
        val question: String = "",
        @property:AI(label = "Context", description = "Who is asking the Question")
        val context: String = ""
    )

    @Serializable
    @AI(label = "The Answer", description = "Holds the answer to the question.")
    class Answer(
        @property:AI(label = "Answer", description = "A resoundingly deep answer to the question")
        val answer: String = ""
    )

    interface QuestionAPI {
        @AI(label = "Ask", description = "Provide an in depth answer to this question, within the context")
        fun askStruct(
            question: Question?
        ): Future<Answer?>

        @AI(label = "AskString", description = "Provide an in depth answer to this question, within the context")
        fun askString(
            question: Question?
        ): Future<String?>
    }

    @Test
    public fun testStubApi() {
        val api = AiApiBuilder(QuestionAPI::class)
            .setAdapter(StubAdapter())
            .build();

        val result = api.askStruct(Question("What is the meaning of life", "A curious student"))
        val answer = result.get()
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    public fun testJsonApi() {
        val answer = AiApiBuilder(QuestionAPI::class)
            .setAdapter(OpenAiAdapter())
            .build()
            .askStruct(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer?.answer)
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    public fun testStringApi() {
        val answer = AiApiBuilder(QuestionAPI::class)
            .setAdapter(OpenAiAdapter())
            .build()
            .askString(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer)
        assertNotNull(answer, "There is no answer for the shimmer")
    }
}