package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.StubAdapter
import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.interfaces.Memorize
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

// Import Swagger/OpenAPI annotations
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import org.junit.jupiter.api.Assertions.*

class AiApiShimmerTest {

    @Serializable
    @Schema(title = "Question", description = "Holds info about the question")
    class Question(
        @field:Schema(title = "Question", description = "The question to be asked")
        val question: String = "",
        @field:Schema(title = "Context", description = "Who is asking the Question")
        val context: String = ""
    )

    @Serializable
    @Schema(title = "The Answer", description = "Holds the answer to the question.")
    class Answer(
        @field:Schema(title = "Answer", description = "A resoundingly deep answer to the question", )
        val answer: String = ""
    )

    interface QuestionAPI {
        @Operation(
            summary = "Ask",
            description = "Provide an in-depth answer to the question within its context."
        )

        @ApiResponse(
            description = "The answer as a struct",
            content = [Content(schema = Schema(implementation = Answer::class))]
        )

        fun askStruct(
            @Parameter(description = "The question and its context for the API call")
            question: Question?
        ): Future<Answer?>

        @Operation(
            summary = "AskString",
            description = "Provide an in-depth answer to the question within its context, returning a string response."
        )

        @ApiResponse(
            description = "The answer as a string",
            content = [Content(schema = Schema(implementation = String::class))],
        )

        @Memorize("The last answer to the question.")
        fun askString(
            @Parameter(description = "The question and its context for the API call")
            question: Question?
        ): Future<String?>
    }

    @Test
    fun testStubApi() {
        val api = ShimmerBuilder(QuestionAPI::class)
            .setAdapter(StubAdapter())
            .build()

        val result = api.askStruct(Question("What is the meaning of life", "A curious student"))
        val answer = result.get()
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    fun testJsonApi() {
        val answer = ShimmerBuilder(QuestionAPI::class)
            .setAdapter(OpenAiAdapter())
            .build()
            .askStruct(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer?.answer)
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    fun testStringApi() {
        val adapter = OpenAiAdapter<QuestionAPI>();
        val answer = ShimmerBuilder(QuestionAPI::class)
            .setAdapter(adapter)
            .build()
            .askString(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer)
        assertNotNull(answer, "There is no answer for the shimmer")
        assertEquals(adapter.getMemoryMap().size, 1, "we should have one memory")
    }
}
