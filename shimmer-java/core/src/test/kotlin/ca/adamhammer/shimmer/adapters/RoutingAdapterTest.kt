package ca.adamhammer.shimmer.adapters

import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.test.MockAdapter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoutingAdapterTest {

    @Test
    fun `routes requests based on method name`() = runBlocking {
        val textAdapter = MockAdapter.scripted("text response")
        val imageAdapter = MockAdapter.scripted("image response")

        val router = RoutingAdapter { context ->
            if (context.methodName == "generateImage") imageAdapter else textAdapter
        }

        val textContext = PromptContext(
            systemInstructions = "",
            methodInvocation = "",
            memory = emptyMap(),
            methodName = "generateText"
        )

        val imageContext = PromptContext(
            systemInstructions = "",
            methodInvocation = "",
            memory = emptyMap(),
            methodName = "generateImage"
        )

        val textResult = router.handleRequest(textContext, String::class)
        val imageResult = router.handleRequest(imageContext, String::class)

        assertEquals("text response", textResult)
        assertEquals("image response", imageResult)
    }
}
