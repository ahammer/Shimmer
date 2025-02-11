package com.adamhammer.ai_shimmer

import StubAdapter

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

class AiApiShimmerTest {

 @Serializable
 class Question(val text: String = "", val context: String = "")

 @Serializable
 class Answer(val text: String = "")

 interface QuestionAPI {
  fun ask(question: Question?): Future<Answer?>
 }

 @Test
  public fun testApi() {
   val api = AiApiBuilder(QuestionAPI::class)
               .setAdapter(StubAdapter())
               .build();

   val result = api.ask(Question("What is the meaning of life", "A curious student"))
   val answer = result.get()
   assertNotNull(answer, "There is no answer for the shimmer")
  }
 }