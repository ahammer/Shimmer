package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.adapters.GeminiAdapter
import ca.adamhammer.shimmer.annotations.AiOperation
import ca.adamhammer.shimmer.annotations.AiParameter
import ca.adamhammer.shimmer.annotations.AiResponse
import ca.adamhammer.shimmer.annotations.AiSchema
import ca.adamhammer.shimmer.annotations.Memorize
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class BasicResultTest {

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
    @Tag("live")
    fun testJsonApi() {
        val answer = ShimmerBuilder(QuestionAPI::class)
            .setAdapterClass(GeminiAdapter::class)
            .build().api
            .askStruct(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer?.answer)
        assertNotNull(answer, "There is no answer for the shimmer")
    }

    @Test
    @Tag("live")
    fun testStringApi() {
        val answer = ShimmerBuilder(QuestionAPI::class)
            .setAdapterClass(GeminiAdapter::class)
            .build().api
            .askString(Question("What is the greatest rodent?", "A small insect asks this question"))
            .get()

        println(answer)
        assertNotNull(answer, "There is no answer for the shimmer")
    }
}
