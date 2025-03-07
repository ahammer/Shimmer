package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlinx.serialization.Serializable

// Import new custom annotations
import com.adamhammer.ai_shimmer.annotations.AiSchema
import com.adamhammer.ai_shimmer.annotations.AiOperation
import com.adamhammer.ai_shimmer.annotations.AiParameter
import com.adamhammer.ai_shimmer.annotations.AiResponse
import org.junit.jupiter.api.Assertions.*

// Define an enum for card rank including an UNDEFINED value.
@Serializable
@AiSchema(title = "CardRank", description = "The rank of a playing card")
enum class CardRank {
    UNDEFINED,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING,
    ACE
}

// Define an enum for card suit including an UNDEFINED value.
@Serializable
@AiSchema(title = "CardSuit", description = "The suit of a playing card")
enum class CardSuit {
    UNDEFINED,
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

// Define the response class which holds the drawn card information.
@Serializable
@AiSchema(
    title = "HigherCardResponse",
    description = "The card drawn which is higher than the provided card. " +
            "If no higher card is available, both rank and suit will be set to UNDEFINED."
)
data class HigherCardResponse(
    @field:AiSchema(title = "Rank", description = "The rank of the drawn card")
    val rank: CardRank = CardRank.UNDEFINED,

    @field:AiSchema(title = "Suit", description = "The suit of the drawn card")
    val suit: CardSuit = CardSuit.UNDEFINED
)

// Define the API interface for drawing a higher card.
interface HigherCardAPI {
    @AiOperation(
        summary = "Draw Higher Card",
        description = "Draws a card with a higher rank than the given card. " +
                "If no higher card is available, returns a card with undefined rank and suit."
    )
    @AiResponse(
        description = "The drawn card as an object with rank and suit enums",
        responseClass = HigherCardResponse::class
    )
    fun drawHigherCard(
        @AiParameter(description = "The current card value as an integer")
        value: Int,

        @AiParameter(description = "The current card suit as a card suit enum")
        suit: CardSuit
    ): Future<HigherCardResponse>
}

// Test cases for the HigherCardAPI.
class HigherCardApiTest {

    @Test
    fun testDrawHigherCardDefined() {
        val api = ShimmerBuilder(HigherCardAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api

        // Simulate a scenario where a higher card is available.
        val response = api.drawHigherCard(5, CardSuit.HEARTS).get()
        assertNotNull(response, "Response should not be null")
        // Verify that the response contains a defined card (i.e. not UNDEFINED).
        assertNotEquals(CardRank.UNDEFINED, response.rank, "Expected a defined rank")
        assertNotEquals(CardSuit.UNDEFINED, response.suit, "Expected a defined suit")
    }

    @Test
    fun testDrawHigherCardUndefined() {
        val api = ShimmerBuilder(HigherCardAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api

        // Simulate a scenario where no higher card is available (e.g., highest card provided).
        val response = api.drawHigherCard(13, CardSuit.CLUBS).get() // Assuming 13 represents ACE.
        assertNotNull(response, "Response should not be null")
        // Verify that both enums are set to UNDEFINED when no higher card exists.
        assertEquals(CardRank.UNDEFINED, response.rank, "Expected undefined rank when no higher card is available")
        assertEquals(CardSuit.UNDEFINED, response.suit, "Expected undefined suit when no higher card is available")
    }
}
